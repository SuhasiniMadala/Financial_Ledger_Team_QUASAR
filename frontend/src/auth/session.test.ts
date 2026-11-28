import { describe, expect, it } from 'vitest';
import { clearSession, loadSession, saveSession, sessionFromLogin } from './session';

const login = {
  accessToken: 'h.p.s',
  tokenType: 'Bearer',
  expiresInSeconds: 3600,
  accountId: 'U-1',
  role: 'USER' as const,
  displayName: 'Asha Rao',
};

describe('session', () => {
  it('keeps the display name for the top bar and computes the expiry', () => {
    const s = sessionFromLogin(login, 1_000);
    expect(s).toEqual({ token: 'h.p.s', accountId: 'U-1', role: 'USER', displayName: 'Asha Rao', expiresAt: 3_601_000 });
  });

  it('survives a reload until it expires', () => {
    const s = sessionFromLogin(login, 1_000);
    saveSession(s);
    expect(loadSession(sessionStorage, 2_000)).toEqual(s);
    expect(loadSession(sessionStorage, 3_601_000)).toBeNull();
    expect(sessionStorage.length).toBe(0);
  });

  it('ignores corrupted storage and logout clears it', () => {
    sessionStorage.setItem('financial-ledger.session', '{not json');
    expect(loadSession()).toBeNull();
    saveSession(sessionFromLogin(login));
    clearSession();
    expect(loadSession()).toBeNull();
  });
});
