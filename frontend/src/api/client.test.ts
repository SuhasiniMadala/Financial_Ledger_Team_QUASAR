import { describe, expect, it, vi } from 'vitest';
import { buildUrl, createApiClient } from './client';
import { ApiError, isRetryable } from './errors';

function jsonResponse(status: number, body: unknown, headers: Record<string, string> = {}) {
  return new Response(body === undefined ? '' : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  });
}

describe('createApiClient', () => {
  it('builds /api/v1 URLs and drops empty query values', () => {
    expect(buildUrl('', '/transactions', { page: 0, size: 20, type: '', walletId: undefined })).toBe(
      '/api/v1/transactions?page=0&size=20',
    );
  });

  it('sends the bearer token and JSON body', async () => {
    const fetchImpl = vi.fn(async () => jsonResponse(201, { walletId: 'W-1' }));
    const client = createApiClient({ getToken: () => 'abc', fetchImpl });
    await client.post('/wallets', { currency: 'INR' });
    const [url, init] = fetchImpl.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('/api/v1/wallets');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer abc');
    expect(init.body).toBe('{"currency":"INR"}');
  });

  it('sends the Idempotency-Key and reports a replay (FR-TRF-05)', async () => {
    const fetchImpl = vi.fn(async () => jsonResponse(201, { transactionId: 'T-1' }, { 'Idempotent-Replayed': 'true' }));
    const client = createApiClient({ fetchImpl });
    const result = await client.postFinancial('/transfers', { amountMinor: 100 }, 'key-12345678');
    const [, init] = fetchImpl.mock.calls[0] as unknown as [string, RequestInit];
    expect((init.headers as Record<string, string>)['Idempotency-Key']).toBe('key-12345678');
    expect(result).toEqual({ data: { transactionId: 'T-1' }, replayed: true });
  });

  it('turns the backend error body into an ApiError with code and failed transaction ID', async () => {
    const body = { code: 'INSUFFICIENT_FUNDS', message: 'low', transactionId: 'T-FAILED00001' };
    const client = createApiClient({ fetchImpl: async () => jsonResponse(422, body, { 'Idempotent-Replayed': 'true' }) });
    const error = await client.postFinancial('/transfers', {}, 'key-12345678').catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ kind: 'http', status: 422, code: 'INSUFFICIENT_FUNDS', transactionId: 'T-FAILED00001', replayed: true });
    expect(isRetryable(error)).toBe(false);
  });

  it('treats 503 SERVICE_BUSY as retryable (UI-04)', async () => {
    const client = createApiClient({ fetchImpl: async () => jsonResponse(503, { code: 'SERVICE_BUSY', message: 'busy' }, { 'Retry-After': '1' }) });
    const error = await client.get('/wallets').catch((e: unknown) => e);
    expect(isRetryable(error)).toBe(true);
  });

  it('treats a 5xx without a ledger error body (dev proxy, backend down) as a network error', async () => {
    const client = createApiClient({ fetchImpl: async () => new Response('Bad gateway', { status: 502 }) });
    const error = await client.get('/wallets').catch((e: unknown) => e);
    expect(error).toMatchObject({ kind: 'network', status: 502 });
    expect(isRetryable(error)).toBe(true);
  });

  it('reports a thrown fetch as a network error', async () => {
    const client = createApiClient({ fetchImpl: async () => Promise.reject(new TypeError('Failed to fetch')) });
    await expect(client.get('/wallets')).rejects.toMatchObject({ kind: 'network' });
  });

  it('gives up after the timeout', async () => {
    const fetchImpl = (_: RequestInfo | URL, init?: RequestInit) =>
      new Promise<Response>((_resolve, reject) => {
        init?.signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')));
      });
    const client = createApiClient({ fetchImpl, timeoutMs: 20 });
    await expect(client.get('/wallets')).rejects.toMatchObject({ kind: 'timeout' });
  });

  it('logs out on 401 only when a token was sent (a wrong password is not a session expiry)', async () => {
    const onUnauthorized = vi.fn();
    const unauthenticated = async () => jsonResponse(401, { code: 'UNAUTHENTICATED', message: 'no' });
    await createApiClient({ fetchImpl: unauthenticated, onUnauthorized, getToken: () => null })
      .post('/auth/login', {})
      .catch(() => undefined);
    expect(onUnauthorized).not.toHaveBeenCalled();
    await createApiClient({ fetchImpl: unauthenticated, onUnauthorized, getToken: () => 'expired' })
      .get('/wallets')
      .catch(() => undefined);
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });
});
