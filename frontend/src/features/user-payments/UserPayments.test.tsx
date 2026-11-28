import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import type { FinancialResult } from '../../api/client';
import type { TransactionReceipt, Wallet } from '../../api/types';
import { financial, fakeClient, httpError, networkError } from '../../test/fakeClient';
import { renderWithProviders } from '../../test/renderWithProviders';
import { navFor } from '../registry';
import type { UserTransaction, UserTransactionDetail } from './api';
import { HistoryDetailPage } from './HistoryDetailPage';
import { HistoryPage } from './HistoryPage';
import { PayMerchantPage } from './PayMerchantPage';
import { TransferPage } from './TransferPage';

const wallets: Wallet[] = [
  { walletId: 'W-1001000000', currency: 'INR', balanceMinor: 500000, balanceFormatted: '', status: 'ACTIVE', createdAt: '' },
  { walletId: 'W-1009000000', currency: 'USD', balanceMinor: 100, balanceFormatted: '', status: 'FROZEN', createdAt: '' },
];

const receipt = (overrides: Partial<TransactionReceipt> = {}): TransactionReceipt => ({
  transactionId: 'T-501000000000',
  type: 'TRANSFER',
  status: 'COMPLETED',
  amountMinor: 100000,
  amountFormatted: '₹1,000.00',
  currency: 'INR',
  sourceAccountId: 'W-1001000000',
  destinationAccountId: 'W-1002000000',
  createdAt: '2026-11-28T10:00:00Z',
  ...overrides,
});

describe('USER navigation (SRS 3.1)', () => {
  it('has Dashboard, Transfer, Pay Merchant and History in SRS order', () => {
    expect(navFor('USER').map((n) => n.label)).toEqual(['Dashboard', 'Transfer', 'Pay Merchant', 'History']);
  });
});

async function fillTransfer(amount: string, destination = 'w-1002000000') {
  await userEvent.selectOptions(await screen.findByLabelText('From wallet'), 'W-1001000000');
  await userEvent.type(screen.getByLabelText('To wallet ID'), destination);
  await userEvent.type(screen.getByLabelText('Amount in rupees'), amount);
}

describe('TransferPage (FR-TRF-01..14, UI-02, UI-04, UI-05)', () => {
  it('offers only ACTIVE wallets as the source (BR-07)', async () => {
    const { client } = fakeClient({ 'GET /wallets': wallets });
    renderWithProviders(<TransferPage />, { client });
    const options = (await screen.findAllByRole('option')).map((o) => o.textContent);
    expect(options).toEqual(['Choose a wallet', 'W-1001000000 - INR - ₹5,000.00']);
  });

  it('sends integer paise with a new idempotency key and shows the receipt (SRS 4.3.2 #1)', async () => {
    const { client, calls } = fakeClient({ 'GET /wallets': wallets, 'POST /transfers': financial(receipt()) });
    renderWithProviders(<TransferPage />, { client });
    await fillTransfer('1,000.00');
    await userEvent.type(screen.getByLabelText(/Description/), 'rent share');
    await userEvent.click(screen.getByRole('button', { name: 'Send money' }));
    expect(await screen.findByText('Transfer completed')).toBeInTheDocument();
    const post = calls.find((c) => c.method === 'POST');
    expect(post?.body).toEqual({ sourceWalletId: 'W-1001000000', destinationWalletId: 'W-1002000000', amountMinor: 100000, description: 'rent share' });
    expect(post?.key).toMatch(/^[A-Za-z0-9_-]{8,64}$/);
    expect(screen.getByRole('link', { name: 'T-501000000000' })).toHaveAttribute('href', '/user/history/T-501000000000');
  });

  it('validates amounts and wallet IDs before sending (SRS 4.3.2 #6)', async () => {
    const { client } = fakeClient({ 'GET /wallets': wallets });
    renderWithProviders(<TransferPage />, { client });
    await fillTransfer('100.5', 'W-12');
    await userEvent.click(screen.getByRole('button', { name: 'Send money' }));
    expect(screen.getByRole('alert')).toHaveTextContent('W- followed by 10');
    await userEvent.clear(screen.getByLabelText('To wallet ID'));
    await userEvent.type(screen.getByLabelText('To wallet ID'), 'W-1002000000');
    await userEvent.clear(screen.getByLabelText('Amount in rupees'));
    await userEvent.type(screen.getByLabelText('Amount in rupees'), '0');
    await userEvent.click(screen.getByRole('button', { name: 'Send money' }));
    expect(screen.getByRole('alert')).toHaveTextContent('at least ₹0.01');
    expect(client.postFinancial).not.toHaveBeenCalled();
  });

  it('disables the button while sending (UI-05)', async () => {
    let release: (r: FinancialResult<TransactionReceipt>) => void = () => undefined;
    const { client } = fakeClient({
      'GET /wallets': wallets,
      'POST /transfers': () => new Promise((resolve) => (release = resolve)),
    });
    renderWithProviders(<TransferPage />, { client });
    await fillTransfer('10');
    await userEvent.click(screen.getByRole('button', { name: 'Send money' }));
    expect(await screen.findByRole('button', { name: 'Sending...' })).toBeDisabled();
    release(financial(receipt()));
    expect(await screen.findByText('Transfer completed')).toBeInTheDocument();
    expect(client.postFinancial).toHaveBeenCalledTimes(1);
  });

  it('retries a timed-out transfer with the same key; the backend replays it (SRS 4.3.2 #2)', async () => {
    let attempt = 0;
    const { client, calls } = fakeClient({
      'GET /wallets': wallets,
      'POST /transfers': () => {
        attempt += 1;
        if (attempt === 1) throw networkError();
        return financial(receipt(), true);
      },
    });
    renderWithProviders(<TransferPage />, { client });
    await fillTransfer('1000');
    await userEvent.click(screen.getByRole('button', { name: 'Send money' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not reach the server');
    await userEvent.click(screen.getByRole('button', { name: 'Retry (same request)' }));
    expect(await screen.findByTestId('replayed')).toBeInTheDocument();
    const posts = calls.filter((c) => c.method === 'POST');
    expect(posts).toHaveLength(2);
    expect(posts[1]?.key).toBe(posts[0]?.key);
    expect(posts[1]?.body).toEqual(posts[0]?.body);
  });

  it('shows insufficient funds with a link to the recorded FAILED transaction (SRS 4.3.2 #5)', async () => {
    const { client } = fakeClient({
      'GET /wallets': wallets,
      'POST /transfers': () => {
        throw httpError(422, { code: 'INSUFFICIENT_FUNDS', message: 'low', transactionId: 'T-502000000000' });
      },
    });
    renderWithProviders(<TransferPage />, { client });
    await fillTransfer('6000');
    await userEvent.click(screen.getByRole('button', { name: 'Send money' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('not enough money');
    expect(screen.getByRole('link', { name: 'Open failed transaction T-502000000000' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Retry (same request)' })).not.toBeInTheDocument();
  });
});

describe('PayMerchantPage (FR-PAY-01..03)', () => {
  it('pays by merchant code with a reference (SRS 4.4.2 #1)', async () => {
    const { client, calls } = fakeClient({
      'GET /wallets': wallets,
      'POST /payments': financial(receipt({ type: 'MERCHANT_PAYMENT', amountMinor: 25000, destinationAccountId: 'W-2001000000' })),
    });
    renderWithProviders(<PayMerchantPage />, { client });
    await userEvent.selectOptions(await screen.findByLabelText('From wallet'), 'W-1001000000');
    await userEvent.type(screen.getByLabelText('Merchant code'), 'mer-7q2k9d');
    await userEvent.type(screen.getByLabelText('Amount in rupees'), '250');
    await userEvent.type(screen.getByLabelText(/Reference/), 'Order 1187');
    await userEvent.click(screen.getByRole('button', { name: 'Pay' }));
    expect(await screen.findByText('Payment completed')).toBeInTheDocument();
    expect(calls.find((c) => c.method === 'POST')?.body).toEqual({
      sourceWalletId: 'W-1001000000',
      merchantCode: 'MER-7Q2K9D',
      amountMinor: 25000,
      reference: 'Order 1187',
    });
  });

  it('explains an unknown merchant code (SRS 4.4.2 #2)', async () => {
    const { client } = fakeClient({
      'GET /wallets': wallets,
      'POST /payments': () => {
        throw httpError(422, { code: 'INVALID_DESTINATION', message: 'x', transactionId: 'T-503000000000' });
      },
    });
    renderWithProviders(<PayMerchantPage />, { client });
    await userEvent.selectOptions(await screen.findByLabelText('From wallet'), 'W-1001000000');
    await userEvent.type(screen.getByLabelText('Merchant code'), 'MER-000000');
    await userEvent.type(screen.getByLabelText('Amount in rupees'), '1');
    await userEvent.click(screen.getByRole('button', { name: 'Pay' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('destination is not valid');
  });
});

const tx = (overrides: Partial<UserTransaction> = {}): UserTransaction => ({
  transactionId: 'T-501000000000',
  type: 'TRANSFER',
  status: 'COMPLETED',
  direction: 'OUTGOING',
  walletId: 'W-1001000000',
  amountMinor: 100000,
  amountFormatted: '',
  currency: 'INR',
  counterpartyName: 'Ravi Kumar',
  createdAt: '2026-11-28T10:00:00Z',
  ...overrides,
});

describe('HistoryPage (FR-LED-11)', () => {
  it('lists own transactions and FAILED attempts, and sends the filters', async () => {
    const { client, calls } = fakeClient({
      'GET /wallets': wallets,
      'GET /transactions': {
        items: [tx(), tx({ transactionId: 'T-502000000000', status: 'FAILED', failureCode: 'INSUFFICIENT_FUNDS', amountMinor: 600000 })],
        page: 0,
        size: 20,
        totalItems: 2,
        totalPages: 1,
      },
    });
    renderWithProviders(<HistoryPage />, { client });
    expect(await screen.findByText('INSUFFICIENT_FUNDS')).toBeInTheDocument();
    expect(screen.getAllByText('Ravi Kumar')).toHaveLength(2);
    await userEvent.selectOptions(screen.getByLabelText('Status'), 'FAILED');
    await userEvent.click(screen.getByRole('button', { name: 'Apply filters' }));
    const last = calls.filter((c) => c.path === '/transactions').at(-1);
    expect(last?.query).toMatchObject({ status: 'FAILED', page: 0, size: 20 });
  });
});

describe('HistoryDetailPage (FR-LED-12)', () => {
  it('shows only the user\'s own entry, counterparty and reversal link (SRS 4.6.2 #6)', async () => {
    const detail: UserTransactionDetail = {
      transaction: tx({ reversedBy: 'T-504000000000' }),
      ownEntries: [{ entryId: 'E-1', sequenceNo: 1, transactionId: 'T-501000000000', accountId: 'W-1001000000', direction: 'DEBIT', amountMinor: 100000, balanceAfterMinor: 400000, createdAt: '' }],
    };
    const { client } = fakeClient({ 'GET /transactions/T-501000000000': detail });
    renderWithProviders(<HistoryDetailPage />, { client, path: '/user/history/:id', url: '/user/history/T-501000000000' });
    expect(await screen.findByText('Ravi Kumar')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'T-504000000000' })).toBeInTheDocument();
    expect(screen.getByText('₹4,000.00')).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(2); // header + own entry only
  });

  it('shows NOT_FOUND for someone else\'s transaction (FR-AUTH-09)', async () => {
    const { client } = fakeClient({
      'GET /transactions/T-999000000000': () => {
        throw httpError(404, { code: 'NOT_FOUND', message: 'x' });
      },
    });
    renderWithProviders(<HistoryDetailPage />, { client, path: '/user/history/:id', url: '/user/history/T-999000000000' });
    expect(await screen.findByRole('alert')).toHaveTextContent('does not belong to you');
  });
});
