import { useCallback, useEffect, useRef, useState } from 'react';
import type { ApiClient } from '../api/client';
import { ApiError, toApiError } from '../api/errors';
import { useApiClient } from '../auth/AuthContext';

export interface QueryState<T> {
  data?: T;
  error?: ApiError;
  loading: boolean;
  reload: () => void;
}

/**
 * Loads data for a screen. `key` must change whenever the request changes (filters, page, ID); the latest
 * answer wins, so a slow old response never overwrites a newer one.
 */
export function useApiQuery<T>(key: string, load: (client: ApiClient) => Promise<T>): QueryState<T> {
  const client = useApiClient();
  const loadRef = useRef(load);
  const [version, setVersion] = useState(0);
  const [state, setState] = useState<{ data?: T; error?: ApiError; loading: boolean }>({ loading: true });

  useEffect(() => {
    loadRef.current = load;
  });

  useEffect(() => {
    let current = true;
    setState((previous) => ({ data: previous.data, loading: true }));
    loadRef.current(client).then(
      (data) => current && setState({ data, loading: false }),
      (error: unknown) => current && setState({ error: toApiError(error), loading: false }),
    );
    return () => {
      current = false;
    };
  }, [client, key, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { ...state, reload };
}
