import type { FormEvent, ReactNode } from 'react';
import type { ApiError } from '../api/errors';
import { ErrorBanner, ReplayedNote } from './Feedback';

/**
 * Wrapper used by every money-moving form. It disables the submit button while the request is in flight (UI-05)
 * and offers Retry - which re-sends the same Idempotency-Key - only after a network error, timeout or 503 (UI-04).
 */
export function FinancialForm({
  title,
  submitLabel,
  inFlight,
  canRetry,
  error,
  replayed,
  onSubmit,
  onRetry,
  validationMessage,
  children,
}: {
  title: string;
  submitLabel: string;
  inFlight: boolean;
  canRetry: boolean;
  error?: ApiError;
  replayed: boolean;
  onSubmit: () => void;
  onRetry: () => void;
  validationMessage?: string;
  children: ReactNode;
}) {
  const handle = (event: FormEvent) => {
    event.preventDefault();
    onSubmit();
  };
  return (
    <form className="card form" onSubmit={handle} noValidate>
      <h2>{title}</h2>
      {children}
      {validationMessage && (
        <p className="field-error" role="alert">
          {validationMessage}
        </p>
      )}
      <ErrorBanner error={error} />
      {error && <ReplayedNote replayed={replayed} />}
      <div className="actions">
        <button type="submit" disabled={inFlight}>
          {inFlight ? 'Sending...' : submitLabel}
        </button>
        {canRetry && (
          <button type="button" className="secondary" onClick={onRetry} disabled={inFlight}>
            Retry (same request)
          </button>
        )}
      </div>
    </form>
  );
}
