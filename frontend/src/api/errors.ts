/** The standard error body of the backend (SRS 3.4, Table 3-2). */
export interface ApiErrorBody {
  code: string;
  message: string;
  timestamp?: string;
  path?: string;
  requestId?: string;
  /** Present when the backend recorded a FAILED transaction (422 on transfers and payments, FR-TRF-13). */
  transactionId?: string;
}

/**
 * - `http`: the backend answered with its standard error body.
 * - `network`: no usable answer (backend down, proxy error, connection reset).
 * - `timeout`: the client gave up waiting.
 */
export type ApiErrorKind = 'http' | 'network' | 'timeout';

export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status?: number;
  readonly code?: string;
  readonly transactionId?: string;
  /** True when the backend replayed a stored 422 (header Idempotent-Replayed: true). */
  readonly replayed: boolean;
  readonly body?: ApiErrorBody;

  constructor(init: { kind: ApiErrorKind; message: string; status?: number; body?: ApiErrorBody; replayed?: boolean }) {
    super(init.message);
    this.name = 'ApiError';
    this.kind = init.kind;
    this.status = init.status;
    this.body = init.body;
    this.code = init.body?.code;
    this.transactionId = init.body?.transactionId;
    this.replayed = init.replayed ?? false;
  }
}

export function isApiErrorBody(value: unknown): value is ApiErrorBody {
  return typeof value === 'object' && value !== null && typeof (value as { code?: unknown }).code === 'string';
}

/**
 * UI-04: a financial request may be retried with the SAME idempotency key after a network error, a timeout or
 * HTTP 503 SERVICE_BUSY. In all of these cases the client does not know (or knows nothing was changed), and the
 * backend makes the retry safe.
 */
export function isRetryable(error: unknown): boolean {
  if (!(error instanceof ApiError)) {
    return false;
  }
  return error.kind === 'network' || error.kind === 'timeout' || error.status === 503;
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }
  return new ApiError({ kind: 'network', message: error instanceof Error ? error.message : 'Unknown error' });
}
