import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { fakeClient, httpError } from '../../test/fakeClient';
import { renderWithProviders, testSession } from '../../test/renderWithProviders';
import type { AccountSummary } from './api';
import { DirectoryPage } from './DirectoryPage';
import { WalletsPage } from './WalletsPage';

const account = (accountId: string, type: AccountSummary['type'], status: AccountSummary['status'], balanceMinor = 0): AccountSummary => ({
  accountId,
  ownerId: type === 'SYSTEM_FUNDING' ? undefined : 'U-1',
  ownerName: type === 'SYSTEM_FUNDING' ? undefined : 'Asha Rao',
  type,
  currency: 'INR',
  balanceMinor,
  balanceFormatted: '',
  status,
  createdAt: '2026-11-28T10:00:00Z',
});

const page = <T,>(items: T[]) => ({ items, page: 0, size: 20, totalItems: items.length, totalPages: 1 });
const admin = testSession('ADMIN', 'Platform Admin');

describe('WalletsPage (FR-ADM-02, FR-ADM-04/05)', () => {
  it('lists every account including System Funding, with only the allowed actions', async () => {
    const { client } = fakeClient({
      'GET /admin/accounts': page([
        account('SYS-FUND-INR', 'SYSTEM_FUNDING', 'ACTIVE', 500000),
        account('W-1001000000', 'USER_WALLET', 'ACTIVE', 500000),
        account('W-1002000000', 'USER_WALLET', 'FROZEN'),
        account('W-1003000000', 'USER_WALLET', 'DEACTIVATED'),
      ]),
    });
    renderWithProviders(<WalletsPage />, { client, session: admin });
    const rows = await screen.findAllByRole('row');
    const actions = (id: string) =>
      within(rows.find((r) => r.textContent?.includes(id)) as HTMLElement)
        .queryAllByRole('button')
        .map((b) => b.textContent);
    expect(actions('SYS-FUND-INR')).toEqual([]);
    expect(actions('W-1001000000')).toEqual(['Freeze', 'Deactivate']);
    expect(actions('W-1002000000')).toEqual(['Unfreeze', 'Deactivate']);
    expect(actions('W-1003000000')).toEqual([]);
    expect(screen.getByText('Platform (system)')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Test funding' })).toHaveLength(1);
  });

  it('requires a reason and sends it with the status change', async () => {
    const { client, calls } = fakeClient({
      'GET /admin/accounts': page([account('W-1002000000', 'USER_WALLET', 'ACTIVE')]),
      'POST /admin/wallets/W-1002000000/freeze': { walletId: 'W-1002000000', currency: 'INR', balanceMinor: 0, balanceFormatted: '₹0.00', status: 'FROZEN', createdAt: '' },
    });
    renderWithProviders(<WalletsPage />, { client, session: admin });
    await userEvent.click(await screen.findByRole('button', { name: 'Freeze' }));
    const confirm = screen.getByRole('button', { name: 'Confirm freeze' });
    expect(confirm).toBeDisabled();
    await userEvent.type(screen.getByLabelText(/Reason/), 'reported lost device');
    await userEvent.click(confirm);
    expect(await screen.findByText('W-1002000000 is now FROZEN.')).toBeInTheDocument();
    expect(calls.find((c) => c.method === 'POST')?.body).toEqual({ reason: 'reported lost device' });
  });

  it('shows the 409 message for an illegal transition', async () => {
    const { client } = fakeClient({
      'GET /admin/accounts': page([account('W-1002000000', 'USER_WALLET', 'FROZEN')]),
      'POST /admin/wallets/W-1002000000/unfreeze': () => {
        throw httpError(409, { code: 'INVALID_STATE_TRANSITION', message: 'no' });
      },
    });
    renderWithProviders(<WalletsPage />, { client, session: admin });
    await userEvent.click(await screen.findByRole('button', { name: 'Unfreeze' }));
    await userEvent.type(screen.getByLabelText(/Reason/), 'try again');
    await userEvent.click(screen.getByRole('button', { name: 'Confirm unfreeze' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('status change is not allowed');
  });
});

describe('DirectoryPage (FR-ADM-01)', () => {
  it('lists users and merchants with merchant codes', async () => {
    const { client } = fakeClient({
      'GET /admin/users': page([{ accountId: 'U-1', name: 'Asha Rao', email: 'asha@example.test', registeredAt: '2026-11-28T10:00:00Z' }]),
      'GET /admin/merchants': page([{ accountId: 'U-2', name: 'Campus Cafe', email: 'cafe@example.test', registeredAt: '2026-11-28T10:00:00Z', merchantCode: 'MER-7Q2K9D' }]),
    });
    renderWithProviders(<DirectoryPage />, { client, session: admin });
    expect(await screen.findByText('asha@example.test')).toBeInTheDocument();
    expect(await screen.findByText('MER-7Q2K9D')).toBeInTheDocument();
  });
});
