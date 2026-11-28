import { vi } from 'vitest';
import type { ApiClient, FinancialResult, Query } from '../api/client';
import { ApiError, type ApiErrorBody } from '../api/errors';

/**
 * A fake ApiClient for component tests (no network). Routes are "METHOD /path" with the path as the screen sends it.
 * A handler returns the response data, or throws (use httpError / networkError).
 * Financial POSTs record their idempotency keys so tests can check UI-04.
 */
export type Handler = (args: { query?: Query; body?: unknown; key?: string }) => unknown;

export function fakeClient(routes: Record<string, Handler | object | string | number | boolean | null>) {
  const calls: { method: string; path: string; query?: Query; body?: unknown; key?: string }[] = [];

  const resolve = async (method: string, path: string, args: { query?: Query; body?: unknown; key?: string }) => {
    calls.push({ method, path, ...args });
    const route = `${method} ${path}`;
    if (!(route in routes)) {
      throw new Error(`fakeClient: no handler for ${route}`);
    }
    const handler = routes[route];
    return typeof handler === 'function' ? await (handler as Handler)(args) : handler;
  };

  const client: ApiClient = {
    get: vi.fn((path: string, query?: Query) => resolve('GET', path, { query })) as ApiClient['get'],
    post: vi.fn((path: string, body?: unknown) => resolve('POST', path, { body })) as ApiClient['post'],
    postFinancial: vi.fn(async (path: string, body: unknown, key: string) => {
      const data = await resolve('POST', path, { body, key });
      return data as FinancialResult<unknown>;
    }) as ApiClient['postFinancial'],
  };
  return { client, calls };
}

export function httpError(status: number, body: ApiErrorBody, replayed = false): ApiError {
  return new ApiError({ kind: 'http', status, body, replayed, message: body.message });
}

export function networkError(): ApiError {
  return new ApiError({ kind: 'network', message: 'Failed to fetch' });
}

export function financial<T>(data: T, replayed = false): FinancialResult<T> {
  return { data, replayed };
}
