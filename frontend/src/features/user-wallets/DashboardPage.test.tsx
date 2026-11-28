import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import type { Wallet } from '../../api/types';
import { fakeClient, httpError } from '../../test/fakeClient';
import { renderWithProviders } from '../../test/renderWithProviders';
import { DashboardPage } from './DashboardPage';

const wallet = (id: string, balanceMinor: number, currency = 'INR'): Wallet => ({
  walletId: id,
  currency,
  balanceMinor,
  balanceFormatted: '',
  status: 'ACTIVE',
  createdAt: '2026-11-28T10:00:00Z',
});

describe('DashboardPage (FR-WAL-01/03/06, UI-01)', () => {
  it('lists own wallets with rupee formatting and raw paise', async () => {
    const { client } = fakeClient({ 'GET /wallets': [wallet('W-1001000000', 10000000)] });
    renderWithProviders(<DashboardPage />, { client });
    expect(await screen.findByText('₹1,00,000.00')).toBeInTheDocument();
    expect(screen.getByText('10000000')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Welcome, Asha Rao' })).toBeInTheDocument();
  });

  it('creates a wallet and reloads the list', async () => {
    let wallets: Wallet[] = [];
    const { client, calls } = fakeClient({
      'GET /wallets': () => wallets,
      'POST /wallets': ({ body }) => {
        wallets = [wallet('W-1001000000', 0, (body as { currency: string }).currency)];
        return wallets[0];
      },
    });
    renderWithProviders(<DashboardPage />, { client });
    expect(await screen.findByText(/no wallets yet/)).toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText('Currency'), 'USD');
    await userEvent.click(screen.getByRole('button', { name: 'Create wallet' }));
    expect(await screen.findByText('$0.00')).toBeInTheDocument();
    expect(calls.find((c) => c.method === 'POST')?.body).toEqual({ currency: 'USD' });
  });

  it('explains a second wallet in the same currency (409)', async () => {
    const { client } = fakeClient({
      'GET /wallets': [wallet('W-1001000000', 0)],
      'POST /wallets': () => {
        throw httpError(409, { code: 'DUPLICATE_RESOURCE', message: 'exists' });
      },
    });
    renderWithProviders(<DashboardPage />, { client });
    await userEvent.click(await screen.findByRole('button', { name: 'Create wallet' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('wallet in this currency');
  });
});
