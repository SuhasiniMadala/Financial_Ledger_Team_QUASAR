import type { ReactNode } from 'react';
import { ApiError } from '../api/errors';
import { messageFor } from '../lib/errorMessages';

/** UI-03: every error is shown as a readable sentence, never as raw JSON. */
export function ErrorBanner({ error }: { error?: ApiError }) {
  if (!error) {
    return null;
  }
  return (
    <div className="banner error" role="alert">
      {messageFor(error)}
    </div>
  );
}

export function SuccessBanner({ children }: { children: ReactNode }) {
  return (
    <div className="banner success" role="status">
      {children}
    </div>
  );
}

export function Loading() {
  return <p className="muted">Loading...</p>;
}

/** Shows loading / error / content for a data screen. */
export function DataState<T>({
  loading,
  error,
  data,
  children,
}: {
  loading: boolean;
  error?: ApiError;
  data?: T;
  children: (data: T) => ReactNode;
}) {
  if (error) {
    return <ErrorBanner error={error} />;
  }
  if (data === undefined) {
    return loading ? <Loading /> : null;
  }
  return <>{children(data)}</>;
}

export function StatusBadge({ value }: { value: string }) {
  return <span className={`badge badge-${value.toLowerCase()}`}>{value}</span>;
}

export function ReplayedNote({ replayed }: { replayed: boolean }) {
  return replayed ? (
    <p className="muted" data-testid="replayed">
      This is the stored result of your earlier identical request (Idempotent-Replayed). Nothing was charged twice.
    </p>
  ) : null;
}
