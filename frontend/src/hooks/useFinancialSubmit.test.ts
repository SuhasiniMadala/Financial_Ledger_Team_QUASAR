import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { FinancialResult } from '../api/client';
import { httpError, networkError } from '../test/fakeClient';
import { useFinancialSubmit } from './useFinancialSubmit';

type Body = { amountMinor: number };
const ok = (data: string): FinancialResult<string> => ({ data, replayed: false });

describe('useFinancialSubmit (UI-04, UI-05)', () => {
  it('uses a new UUID key for each new submission', async () => {
    const send = vi.fn(async (_body: Body, _key: string) => ok('T-1'));
    const { result } = renderHook(() => useFinancialSubmit(send));
    await act(() => result.current.submit({ amountMinor: 100 }));
    act(() => result.current.reset());
    await act(() => result.current.submit({ amountMinor: 100 }));
    const [first, second] = send.mock.calls.map((c) => c[1]);
    expect(first).toMatch(/^[A-Za-z0-9_-]{8,64}$/);
    expect(second).not.toBe(first);
  });

  it('retries with the SAME key and body after a network error', async () => {
    const send = vi
      .fn<(body: Body, key: string) => Promise<FinancialResult<string>>>()
      .mockRejectedValueOnce(networkError())
      .mockResolvedValueOnce({ data: 'T-1', replayed: true });
    const { result } = renderHook(() => useFinancialSubmit(send));
    await act(() => result.current.submit({ amountMinor: 500 }));
    expect(result.current.canRetry).toBe(true);
    await act(() => result.current.retry());
    expect(send.mock.calls[1]).toEqual(send.mock.calls[0]);
    expect(result.current.result).toBe('T-1');
    expect(result.current.replayed).toBe(true);
  });

  it('keeps the key when the user presses Submit again with an unchanged form after 503', async () => {
    const busy = httpError(503, { code: 'SERVICE_BUSY', message: 'busy' });
    const send = vi.fn<(body: Body, key: string) => Promise<FinancialResult<string>>>().mockRejectedValueOnce(busy).mockResolvedValue(ok('T-1'));
    const { result } = renderHook(() => useFinancialSubmit(send));
    await act(() => result.current.submit({ amountMinor: 500 }));
    await act(() => result.current.submit({ amountMinor: 500 }));
    expect(send.mock.calls[1]?.[1]).toBe(send.mock.calls[0]?.[1]);
  });

  it('uses a new key when the form was changed after a network error', async () => {
    const send = vi.fn<(body: Body, key: string) => Promise<FinancialResult<string>>>().mockRejectedValueOnce(networkError()).mockResolvedValue(ok('T-2'));
    const { result } = renderHook(() => useFinancialSubmit(send));
    await act(() => result.current.submit({ amountMinor: 500 }));
    await act(() => result.current.submit({ amountMinor: 600 }));
    expect(send.mock.calls[1]?.[1]).not.toBe(send.mock.calls[0]?.[1]);
  });

  it('does not offer retry after a definite answer such as 422', async () => {
    const send = vi.fn(async () => {
      throw httpError(422, { code: 'INSUFFICIENT_FUNDS', message: 'low', transactionId: 'T-FAILED00001' });
    });
    const { result } = renderHook(() => useFinancialSubmit<Body, string>(send));
    await act(() => result.current.submit({ amountMinor: 500 }));
    expect(result.current.canRetry).toBe(false);
    expect(result.current.error?.transactionId).toBe('T-FAILED00001');
  });

  it('ignores a second submit while the first is in flight (double click)', async () => {
    let release: (v: FinancialResult<string>) => void = () => undefined;
    const send = vi.fn(() => new Promise<FinancialResult<string>>((resolve) => (release = resolve)));
    const { result } = renderHook(() => useFinancialSubmit<Body, string>(send));
    let first: Promise<void> = Promise.resolve();
    act(() => {
      first = result.current.submit({ amountMinor: 100 });
    });
    await waitFor(() => expect(result.current.inFlight).toBe(true));
    await act(() => result.current.submit({ amountMinor: 100 }));
    expect(send).toHaveBeenCalledTimes(1);
    await act(async () => {
      release(ok('T-1'));
      await first;
    });
    expect(result.current.inFlight).toBe(false);
  });
});
