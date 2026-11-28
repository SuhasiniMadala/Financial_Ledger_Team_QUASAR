import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import type { ApiClient } from '../api/client';
import type { Role } from '../api/types';
import { AuthContext, type AuthContextValue } from '../auth/AuthContext';
import type { Session } from '../auth/session';

export function testSession(role: Role = 'USER', displayName = 'Asha Rao'): Session {
  return { token: 'test-token', accountId: 'U-TEST', role, displayName, expiresAt: Date.now() + 3_600_000 };
}

/**
 * Renders a screen with a fake client and a logged-in session. `path` is the route pattern (e.g.
 * '/admin/transactions/:id') and `url` the URL to open, so screens that read useParams work.
 */
export function renderWithProviders(
  element: ReactElement,
  options: { client: ApiClient; session?: Session | null; path?: string; url?: string },
) {
  const value: AuthContextValue = {
    session: options.session === undefined ? testSession() : options.session,
    client: options.client,
    login: vi.fn(),
    logout: vi.fn(),
  };
  const path = options.path ?? '/';
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={[options.url ?? path]}>
        <Routes>
          <Route path={path} element={element} />
          <Route path="*" element={<div>other page</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}
