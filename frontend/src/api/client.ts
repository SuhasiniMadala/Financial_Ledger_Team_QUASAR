import { ApiError, isApiErrorBody } from './errors';

/** Every backend path is under this prefix (SRS 2.1). Feature code passes paths such as '/wallets'. */
export const API_PREFIX = '/api/v1';

export type Query = Record<string, string | number | undefined | null>;

/** Result of a request that carries an Idempotency-Key (the starred endpoints of SRS Table 3-1). */
export interface FinancialResult<T> {
  data: T;
  /** True when the backend replayed the stored response (header Idempotent-Replayed: true, FR-TRF-05). */
  replayed: boolean;
}

export interface ApiClient {
  get<T>(path: string, query?: Query): Promise<T>;
  /** Non-financial POST: registration, login, wallet creation, status changes, reconciliation. */
  post<T>(path: string, body?: unknown): Promise<T>;
  /** Financial POST: transfers, payments, test funding, reversals. The caller owns the key (UI-04). */
  postFinancial<T>(path: string, body: unknown, idempotencyKey: string): Promise<FinancialResult<T>>;
}

export interface ApiClientOptions {
  /** '' in the browser (same origin through the Vite proxy); 'http://localhost:8080' in integration tests. */
  baseUrl?: string;
  getToken?: () => string | null;
  /** Called when a request that carried a token gets 401 (expired or invalid session). */
  onUnauthorized?: () => void;
  timeoutMs?: number;
  fetchImpl?: typeof fetch;
}

export const DEFAULT_TIMEOUT_MS = 15_000;

export function buildUrl(baseUrl: string, path: string, query?: Query): string {
  const params = new URLSearchParams();
  for (const [name, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null && value !== '') {
      params.set(name, String(value));
    }
  }
  const qs = params.toString();
  return `${baseUrl}${API_PREFIX}${path}${qs ? `?${qs}` : ''}`;
}

export function createApiClient(options: ApiClientOptions = {}): ApiClient {
  const baseUrl = options.baseUrl ?? '';
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const doFetch = options.fetchImpl ?? ((input: RequestInfo | URL, init?: RequestInit) => fetch(input, init));

  async function request<T>(
    method: 'GET' | 'POST',
    path: string,
    init: { query?: Query; body?: unknown; idempotencyKey?: string },
  ): Promise<FinancialResult<T>> {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (init.body !== undefined) {
      headers['Content-Type'] = 'application/json';
    }
    const token = options.getToken?.() ?? null;
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    if (init.idempotencyKey) {
      headers['Idempotency-Key'] = init.idempotencyKey;
    }

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    let response: Response;
    try {
      response = await doFetch(buildUrl(baseUrl, path, init.query), {
        method,
        headers,
        body: init.body === undefined ? undefined : JSON.stringify(init.body),
        signal: controller.signal,
      });
    } catch (cause) {
      if (controller.signal.aborted) {
        throw new ApiError({ kind: 'timeout', message: `No answer within ${timeoutMs / 1000} seconds` });
      }
      throw new ApiError({ kind: 'network', message: cause instanceof Error ? cause.message : 'Network error' });
    } finally {
      clearTimeout(timer);
    }

    const replayed = response.headers.get('Idempotent-Replayed') === 'true';
    const text = await response.text();
    let parsed: unknown = undefined;
    if (text) {
      try {
        parsed = JSON.parse(text);
      } catch {
        parsed = undefined;
      }
    }

    if (!response.ok) {
      if (!isApiErrorBody(parsed)) {
        // Not the backend's error body: typically the dev proxy answering for a backend that is down.
        // A real backend failure always carries {"code": ...}, so this is treated as a network problem.
        throw new ApiError({ kind: 'network', status: response.status, message: `HTTP ${response.status} without a ledger error body` });
      }
      if (response.status === 401 && token) {
        options.onUnauthorized?.();
      }
      throw new ApiError({ kind: 'http', status: response.status, body: parsed, replayed, message: parsed.message });
    }
    return { data: parsed as T, replayed };
  }

  return {
    get: async <T>(path: string, query?: Query) => (await request<T>('GET', path, { query })).data,
    post: async <T>(path: string, body?: unknown) => (await request<T>('POST', path, { body })).data,
    postFinancial: <T>(path: string, body: unknown, idempotencyKey: string) =>
      request<T>('POST', path, { body, idempotencyKey }),
  };
}
