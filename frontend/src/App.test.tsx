import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { App } from './App';
import type { Role } from './api/types';
import { AuthContext } from './auth/AuthContext';
import { navFor } from './features/registry';
import { fakeClient } from './test/fakeClient';
import { testSession } from './test/renderWithProviders';

function renderApp(url: string, role: Role | null) {
  const { client } = fakeClient({
    'GET /wallets': [],
    'GET /admin/users': { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 },
    'GET /admin/merchants': { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 },
  });
  const session = role ? testSession(role) : null;
  return render(
    <AuthContext.Provider value={{ session, client, login: vi.fn(), logout: vi.fn() }}>
      <MemoryRouter initialEntries={[url]}>
        <App />
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

const navLabels = () =>
  within(screen.getByRole('navigation', { name: 'Main navigation' }))
    .getAllByRole('link')
    .map((a) => a.textContent);

// Each feature's own test file checks its navigation entries; this file checks the shell (SRS 3.1, UI-03).
describe('App shell and role routing (SRS 3.1, UI-03)', () => {
  it('sends a visitor to the login page', () => {
    renderApp('/user/dashboard', null);
    expect(screen.getByRole('heading', { name: 'Log in' })).toBeInTheDocument();
  });

  it('shows product name, account name, Logout and the USER navigation', async () => {
    renderApp('/user/dashboard', 'USER');
    expect(await screen.findByTestId('account-name')).toHaveTextContent('Asha Rao');
    expect(screen.getByText('Financial Ledger')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Logout' })).toBeInTheDocument();
    expect(navLabels()[0]).toBe('Dashboard');
  });

  it('shows the ADMIN navigation', async () => {
    renderApp('/admin/directory', 'ADMIN');
    expect(await screen.findByRole('heading', { name: 'Users and merchants' })).toBeInTheDocument();
    expect(navLabels().slice(0, 2)).toEqual(['Users & Merchants', 'Wallets']);
  });

  it("blocks another role's screens", () => {
    renderApp('/admin/directory', 'USER');
    expect(screen.getByRole('heading', { name: 'Not allowed' })).toBeInTheDocument();
  });

  it('never shows one role the navigation of another', () => {
    const paths = (role: Role) => navFor(role).map((n) => n.path);
    expect(paths('USER').every((p) => p.startsWith('/user/'))).toBe(true);
    expect(paths('MERCHANT').every((p) => p.startsWith('/merchant/'))).toBe(true);
    expect(paths('ADMIN').every((p) => p.startsWith('/admin/'))).toBe(true);
  });
});
