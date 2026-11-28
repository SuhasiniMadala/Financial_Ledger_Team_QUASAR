import type { LoginResponse, Role } from '../api/types';

export interface Session {
  token: string;
  accountId: string;
  role: Role;
  displayName: string;
  /** Epoch milliseconds when the JWT expires (60 minutes, NFR-SEC-02). */
  expiresAt: number;
}

const STORAGE_KEY = 'financial-ledger.session';

/**
 * The session lives in sessionStorage: it survives a page refresh but disappears when the tab is closed.
 * Limitation (ADR-011): any script running in the page could read it. React escapes all output, the client loads
 * no third-party scripts and the token expires after 60 minutes.
 */
export function sessionFromLogin(login: LoginResponse, now: number = Date.now()): Session {
  return {
    token: login.accessToken,
    accountId: login.accountId,
    role: login.role,
    displayName: login.displayName,
    expiresAt: now + login.expiresInSeconds * 1000,
  };
}

export function isExpired(session: Session, now: number = Date.now()): boolean {
  return now >= session.expiresAt;
}

export function loadSession(storage: Storage = sessionStorage, now: number = Date.now()): Session | null {
  const raw = storage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const session = JSON.parse(raw) as Session;
    if (!session.token || !session.role || typeof session.expiresAt !== 'number' || isExpired(session, now)) {
      storage.removeItem(STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    storage.removeItem(STORAGE_KEY);
    return null;
  }
}

export function saveSession(session: Session, storage: Storage = sessionStorage): void {
  storage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearSession(storage: Storage = sessionStorage): void {
  storage.removeItem(STORAGE_KEY);
}

/** Where each role lands after login. */
export const HOME_BY_ROLE: Record<Role, string> = {
  USER: '/user/dashboard',
  MERCHANT: '/merchant/account',
  ADMIN: '/admin/directory',
};
