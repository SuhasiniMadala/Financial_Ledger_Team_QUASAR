import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { createApiClient, type ApiClient } from '../api/client';
import type { LoginResponse } from '../api/types';
import { clearSession, loadSession, saveSession, sessionFromLogin, type Session } from './session';

export interface AuthContextValue {
  session: Session | null;
  /** Client bound to the current session (adds the Bearer token, logs out on 401). */
  client: ApiClient;
  login: (email: string, password: string) => Promise<Session>;
  logout: (notice?: string) => void;
  /** One-time message for the login page, e.g. "Your session has expired". */
  notice?: string;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export const SESSION_EXPIRED = 'Your session has expired. Please log in again.';

export function AuthProvider({ children, baseUrl = '' }: { children: ReactNode; baseUrl?: string }) {
  const [session, setSession] = useState<Session | null>(() => loadSession());
  const [notice, setNotice] = useState<string>();
  const sessionRef = useRef(session);

  useEffect(() => {
    sessionRef.current = session;
  }, [session]);

  const logout = useCallback((message?: string) => {
    clearSession();
    sessionRef.current = null;
    setSession(null);
    setNotice(message);
  }, []);

  const client = useMemo(
    () =>
      createApiClient({
        baseUrl,
        getToken: () => sessionRef.current?.token ?? null,
        onUnauthorized: () => logout(SESSION_EXPIRED),
      }),
    [baseUrl, logout],
  );

  const login = useCallback(
    async (email: string, password: string) => {
      const response = await client.post<LoginResponse>('/auth/login', { email, password });
      const next = sessionFromLogin(response);
      saveSession(next);
      sessionRef.current = next;
      setSession(next);
      setNotice(undefined);
      return next;
    },
    [client],
  );

  // Log out automatically when the 60-minute token expires, even if no request is made.
  useEffect(() => {
    if (!session) {
      return undefined;
    }
    const timer = setTimeout(() => logout(SESSION_EXPIRED), Math.max(0, session.expiresAt - Date.now()));
    return () => clearTimeout(timer);
  }, [session, logout]);

  const value = useMemo(() => ({ session, client, login, logout, notice }), [session, client, login, logout, notice]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return value;
}

export function useApiClient(): ApiClient {
  return useAuth().client;
}

/** For screens that are only rendered inside RequireRole. */
export function useSession(): Session {
  const { session } = useAuth();
  if (!session) {
    throw new Error('useSession used outside a protected route');
  }
  return session;
}
