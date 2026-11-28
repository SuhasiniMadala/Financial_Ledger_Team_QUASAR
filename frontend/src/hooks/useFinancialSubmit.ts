import { useCallback, useEffect, useRef, useState } from 'react';
import type { FinancialResult } from '../api/client';
import { ApiError, isRetryable, toApiError } from '../api/errors';

/** A UUID matches the backend's key rule ^[A-Za-z0-9_-]{8,64}$ (FR-TRF-03). */
export function newIdempotencyKey(): string {
  return crypto.randomUUID();
}

export interface FinancialSubmitState<Res> {
  inFlight: boolean;
  result?: Res;
  replayed: boolean;
  error?: ApiError;
  /** The last attempt failed in a way that is safe to retry with the same key (network, timeout, 503). */
  canRetry: boolean;
}

const IDLE = { inFlight: false, replayed: false, canRetry: false } as const;

/**
 * One hook for every money-moving form: Transfer, Pay Merchant, Test funding, Reversal.
 *
 * UI-04: each new submission gets a new UUID Idempotency-Key. After a network error, timeout or HTTP 503 the
 * SAME key and SAME body are sent again, either with retry() or by pressing Submit again without changing
 * anything. So a request that did commit before the connection dropped is replayed by the backend, never
 * executed twice (FR-TRF-05). Changing any field makes it a new submission with a new key.
 *
 * UI-05: while a request is in flight `inFlight` is true (the button is disabled) and further calls are ignored
 * by a ref guard, so a double click cannot send two requests.
 */
export function useFinancialSubmit<Req, Res>(send: (body: Req, idempotencyKey: string) => Promise<FinancialResult<Res>>) {
  const [state, setState] = useState<FinancialSubmitState<Res>>(IDLE);
  const sendRef = useRef(send);
  const inFlightRef = useRef(false);
  const pendingRef = useRef<{ body: Req; bodyJson: string; key: string } | null>(null);

  useEffect(() => {
    sendRef.current = send;
  }, [send]);

  const run = useCallback(async (body: Req, key: string) => {
    inFlightRef.current = true;
    setState({ inFlight: true, replayed: false, canRetry: false });
    try {
      const response = await sendRef.current(body, key);
      pendingRef.current = null;
      setState({ inFlight: false, result: response.data, replayed: response.replayed, canRetry: false });
    } catch (caught) {
      const error = toApiError(caught);
      const retryable = isRetryable(error);
      if (!retryable) {
        // A definite answer (e.g. 422 INSUFFICIENT_FUNDS, stored and replayable by the backend): the next
        // submission is a new one.
        pendingRef.current = null;
      }
      setState({ inFlight: false, error, canRetry: retryable, replayed: error.replayed });
    } finally {
      inFlightRef.current = false;
    }
  }, []);

  const submit = useCallback(
    async (body: Req) => {
      if (inFlightRef.current) {
        return;
      }
      const bodyJson = JSON.stringify(body);
      const pending = pendingRef.current;
      // Same body after an uncertain failure = the same logical request: keep its key (UI-04).
      const key = pending && pending.bodyJson === bodyJson ? pending.key : newIdempotencyKey();
      pendingRef.current = { body, bodyJson, key };
      await run(body, key);
    },
    [run],
  );

  const retry = useCallback(async () => {
    const pending = pendingRef.current;
    if (inFlightRef.current || !pending) {
      return;
    }
    await run(pending.body, pending.key);
  }, [run]);

  /** Clear the result to start a new, unrelated submission ("Make another transfer"). */
  const reset = useCallback(() => {
    if (inFlightRef.current) {
      return;
    }
    pendingRef.current = null;
    setState(IDLE);
  }, []);

  return { ...state, submit, retry, reset };
}
