import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactElement } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AuthContext, type AuthContextValue } from '../auth/AuthContext';
import { fakeClient, httpError } from '../test/fakeClient';
import { testSession } from '../test/renderWithProviders';
import { LoginPage } from './LoginPage';
import { RegisterPage } from './RegisterPage';

function renderAuth(element: ReactElement, value: Partial<AuthContextValue>) {
  const { client } = fakeClient({});
  return render(
    <AuthContext.Provider value={{ session: null, client, login: vi.fn(), logout: vi.fn(), ...value }}>
      <MemoryRouter initialEntries={['/start']}>
        <Routes>
          <Route path="/start" element={element} />
          <Route path="/user/dashboard" element={<div>user home</div>} />
          <Route path="/admin/directory" element={<div>admin home</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('LoginPage (FR-AUTH-06)', () => {
  it('logs in and opens the home page of the role', async () => {
    const login = vi.fn(async () => testSession('ADMIN'));
    renderAuth(<LoginPage />, { login });
    await userEvent.type(screen.getByLabelText('Email'), ' admin@local.test ');
    await userEvent.type(screen.getByLabelText('Password'), 'secret-password');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(login).toHaveBeenCalledWith('admin@local.test', 'secret-password');
    expect(await screen.findByText('admin home')).toBeInTheDocument();
  });

  it('shows one generic message for a failed login', async () => {
    const login = vi.fn(async () => {
      throw httpError(401, { code: 'UNAUTHENTICATED', message: 'Invalid email or password' });
    });
    renderAuth(<LoginPage />, { login });
    await userEvent.type(screen.getByLabelText('Email'), 'a@b.test');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong-password');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('The email or password is not correct');
  });

  it('shows the session-expired notice', () => {
    renderAuth(<LoginPage />, { notice: 'Your session has expired. Please log in again.' });
    expect(screen.getByText('Your session has expired. Please log in again.')).toBeInTheDocument();
  });
});

describe('RegisterPage (FR-AUTH-01..04)', () => {
  it('rejects a short password before calling the backend', async () => {
    const { client } = fakeClient({});
    renderAuth(<RegisterPage />, { client });
    await userEvent.type(screen.getByLabelText('Display name'), 'Asha Rao');
    await userEvent.type(screen.getByLabelText('Email'), 'asha@example.test');
    await userEvent.type(screen.getByLabelText(/Password/), 'short');
    await userEvent.click(screen.getByRole('button', { name: 'Register' }));
    expect(screen.getByRole('alert')).toHaveTextContent('8 to 72 characters');
    expect(client.post).not.toHaveBeenCalled();
  });

  it('registers a merchant and shows the merchant code and wallet', async () => {
    const { client, calls } = fakeClient({
      'POST /auth/register/merchant': { accountId: 'U-2', role: 'MERCHANT', email: 'cafe@example.test', displayName: 'Campus Cafe', merchantCode: 'MER-7Q2K9D', walletId: 'W-2001000000' },
    });
    renderAuth(<RegisterPage />, { client });
    await userEvent.click(screen.getByLabelText('Merchant'));
    await userEvent.type(screen.getByLabelText('Business name'), 'Campus Cafe');
    await userEvent.type(screen.getByLabelText('Email'), 'cafe@example.test');
    await userEvent.type(screen.getByLabelText(/Password/), 'long-enough-password');
    await userEvent.click(screen.getByRole('button', { name: 'Register' }));
    expect(await screen.findByText('MER-7Q2K9D')).toBeInTheDocument();
    expect(calls[0]?.body).toEqual({ businessName: 'Campus Cafe', email: 'cafe@example.test', password: 'long-enough-password' });
  });

  it('explains a duplicate email (409)', async () => {
    const { client } = fakeClient({
      'POST /auth/register': () => {
        throw httpError(409, { code: 'DUPLICATE_RESOURCE', message: 'exists' });
      },
    });
    renderAuth(<RegisterPage />, { client });
    await userEvent.type(screen.getByLabelText('Display name'), 'Asha Rao');
    await userEvent.type(screen.getByLabelText('Email'), 'asha@example.test');
    await userEvent.type(screen.getByLabelText(/Password/), 'long-enough-password');
    await userEvent.click(screen.getByRole('button', { name: 'Register' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('already exists');
  });
});
