import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import type { TransactionReceipt } from '../../api/types';
import { financial, fakeClient, httpError, networkError } from '../../test/fakeClient';
import { navFor } from '../registry';
import { renderWithProviders, testSession } from '../../test/renderWithProviders';
import type { AdminTransactionDetail, PlatformStatistics, ReconciliationReport } from './api';
import { ReconciliationPage } from './ReconciliationPage';
import { StatisticsPage } from './StatisticsPage';
import { TestFundingPage } from './TestFundingPage';
import { TransactionDetailPage } from './TransactionDetailPage';

const admin = testSession('ADMIN', 'Platform Admin');

describe('ADMIN navigation (SRS 3.1)', () => {
  it('has all five admin screens in SRS order', () => {
    expect(navFor('ADMIN').map((n) => n.label)).toEqual([
      'Users & Merchants',
      'Wallets',
      'Transactions & Ledger',
      'Reconciliation',
      'Statistics',
    ]);
  });
});

const receipt = (overrides: Partial<TransactionReceipt> = {}): TransactionReceipt => ({
  transactionId: 'T-500000000000',
  type: 'TEST_FUNDING',
  status: 'COMPLETED',
  amountMinor: 500000,
  amountFormatted: '₹5,000.00',
  currency: 'INR',
  sourceAccountId: 'SYS-FUND-INR',
  destinationAccountId: 'W-1001000000',
  createdAt: '2026-11-28T10:00:00Z',
  ...overrides,
});

describe('TestFundingPage (FR-WAL-07/08, UI-02, UI-04)', () => {
  const open = (client: ReturnType<typeof fakeClient>['client']) =>
    renderWithProviders(<TestFundingPage />, {
      client,
      session: admin,
      path: '/admin/wallets/:walletId/test-funding',
      url: '/admin/wallets/W-1001000000/test-funding',
    });

  it('sends integer paise with an idempotency key and shows the receipt', async () => {
    const { client, calls } = fakeClient({ 'POST /admin/wallets/W-1001000000/test-funding': financial(receipt()) });
    open(client);
    await userEvent.type(screen.getByLabelText('Amount in rupees'), '5,000.00');
    await userEvent.click(screen.getByRole('button', { name: 'Add simulated funds' }));
    expect(await screen.findByText('Test funding recorded')).toBeInTheDocument();
    expect(screen.getByText('₹5,000.00')).toBeInTheDocument();
    expect(calls[0]?.body).toEqual({ amountMinor: 500000 });
    expect(calls[0]?.key).toMatch(/^[A-Za-z0-9_-]{8,64}$/);
  });

  it('rejects more than two decimals without calling the backend', async () => {
    const { client } = fakeClient({});
    open(client);
    await userEvent.type(screen.getByLabelText('Amount in rupees'), '10.505');
    await userEvent.click(screen.getByRole('button', { name: 'Add simulated funds' }));
    expect(screen.getByRole('alert')).toHaveTextContent('at most two decimal places');
    expect(client.postFinancial).not.toHaveBeenCalled();
  });

  it('retries with the same key after a network error and shows the replay', async () => {
    let attempt = 0;
    const { client, calls } = fakeClient({
      'POST /admin/wallets/W-1001000000/test-funding': () => {
        attempt += 1;
        if (attempt === 1) throw networkError();
        return financial(receipt(), true);
      },
    });
    open(client);
    await userEvent.type(screen.getByLabelText('Amount in rupees'), '5000');
    await userEvent.click(screen.getByRole('button', { name: 'Add simulated funds' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Retry (same request)' }));
    expect(await screen.findByTestId('replayed')).toBeInTheDocument();
    expect(calls[1]?.key).toBe(calls[0]?.key);
  });

  it('explains funding a frozen wallet (422 WALLET_NOT_ACTIVE)', async () => {
    const { client } = fakeClient({
      'POST /admin/wallets/W-1001000000/test-funding': () => {
        throw httpError(422, { code: 'WALLET_NOT_ACTIVE', message: 'frozen' });
      },
    });
    open(client);
    await userEvent.type(screen.getByLabelText('Amount in rupees'), '100');
    await userEvent.click(screen.getByRole('button', { name: 'Add simulated funds' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('frozen or deactivated');
    expect(screen.queryByRole('button', { name: 'Retry (same request)' })).not.toBeInTheDocument();
  });
});

describe('TransactionDetailPage (FR-ADM-03, FR-LED-07..09)', () => {
  const detail = (overrides: Partial<AdminTransactionDetail['transaction']> = {}): AdminTransactionDetail => ({
    transaction: {
      transactionId: 'T-501000000000',
      type: 'TRANSFER',
      status: 'COMPLETED',
      amountMinor: 100000,
      amountFormatted: '₹1,000.00',
      currency: 'INR',
      initiatedBy: 'U-1',
      sourceAccountId: 'W-1001000000',
      destinationAccountId: 'W-1002000000',
      createdAt: '2026-11-28T10:00:00Z',
      ...overrides,
    },
    entries: [
      { entryId: 'E-1', sequenceNo: 1, transactionId: 'T-501000000000', accountId: 'W-1001000000', direction: 'DEBIT', amountMinor: 100000, balanceAfterMinor: 400000, createdAt: '' },
      { entryId: 'E-2', sequenceNo: 2, transactionId: 'T-501000000000', accountId: 'W-1002000000', direction: 'CREDIT', amountMinor: 100000, balanceAfterMinor: 300000, createdAt: '' },
    ],
  });
  const open = (client: ReturnType<typeof fakeClient>['client']) =>
    renderWithProviders(<TransactionDetailPage />, {
      client,
      session: admin,
      path: '/admin/transactions/:id',
      url: '/admin/transactions/T-501000000000',
    });

  it('shows the entries and that debits equal credits', async () => {
    const { client } = fakeClient({ 'GET /admin/transactions/T-501000000000': detail() });
    open(client);
    expect(await screen.findByTestId('balance-check')).toHaveTextContent('balanced');
    expect(screen.getByText('₹4,000.00')).toBeInTheDocument();
  });

  it('requires a 10-500 character reason, then reverses with an idempotency key', async () => {
    const { client, calls } = fakeClient({
      'GET /admin/transactions/T-501000000000': detail(),
      'POST /admin/transactions/T-501000000000/reversal': financial(receipt({ transactionId: 'T-504000000000', type: 'REVERSAL', reversalOfId: 'T-501000000000' })),
    });
    open(client);
    await userEvent.type(await screen.findByLabelText(/Reason/), 'too short');
    await userEvent.click(screen.getByRole('button', { name: 'Reverse' }));
    expect(screen.getByRole('alert')).toHaveTextContent('10 to 500 characters');
    await userEvent.type(screen.getByLabelText(/Reason/), ' - sent to wrong recipient');
    await userEvent.click(screen.getByRole('button', { name: 'Reverse' }));
    expect(await screen.findByRole('link', { name: 'T-504000000000' })).toBeInTheDocument();
    const reversal = calls.find((c) => c.method === 'POST');
    expect(reversal?.body).toEqual({ reason: 'too short - sent to wrong recipient' });
    expect(reversal?.key).toBeTruthy();
  });

  it('offers no reversal for a reversed, failed or reversal transaction', async () => {
    const { client } = fakeClient({ 'GET /admin/transactions/T-501000000000': detail({ reversedBy: 'T-504000000000' }) });
    open(client);
    expect(await screen.findByRole('link', { name: 'T-504000000000' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reverse' })).not.toBeInTheDocument();
  });

  it('shows ALREADY_REVERSED from the backend', async () => {
    const { client } = fakeClient({
      'GET /admin/transactions/T-501000000000': detail(),
      'POST /admin/transactions/T-501000000000/reversal': () => {
        throw httpError(409, { code: 'ALREADY_REVERSED', message: 'x' });
      },
    });
    open(client);
    await userEvent.type(await screen.findByLabelText(/Reason/), 'sent to wrong recipient');
    await userEvent.click(screen.getByRole('button', { name: 'Reverse' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('already been reversed');
  });
});

describe('ReconciliationPage (FR-REC-01..08)', () => {
  const report = (overrides: Partial<ReconciliationReport> = {}): ReconciliationReport => ({
    runId: 'R-1',
    runAt: '2026-11-28T10:00:00Z',
    durationMs: 12,
    result: 'PASS',
    currencies: [{ currency: 'INR', totalDebitsMinor: 925000, totalCreditsMinor: 925000, totalDebitsFormatted: '', totalCreditsFormatted: '' }],
    checked: { accounts: 4, transactions: 6, entries: 10 },
    discrepancies: [],
    ...overrides,
  });

  it('runs and shows a PASS report (SRS 4.7.2 #1)', async () => {
    const { client } = fakeClient({ 'POST /admin/reconciliations': report() });
    renderWithProviders(<ReconciliationPage />, { client, session: admin });
    await userEvent.click(screen.getByRole('button', { name: 'Run reconciliation' }));
    expect(await screen.findByText('PASS')).toBeInTheDocument();
    expect(screen.getByText('4 accounts, 6 transactions, 10 entries')).toBeInTheDocument();
    expect(screen.getAllByText('₹9,250.00')).toHaveLength(2);
  });

  it('lists discrepancies of a FAIL report (SRS 4.7.2 #2)', async () => {
    const { client } = fakeClient({
      'POST /admin/reconciliations': report({
        result: 'FAIL',
        discrepancies: [{ type: 'BALANCE_MISMATCH', subjectId: 'W-1002000000', expectedMinor: 200000, actualMinor: 250000, differenceMinor: 50000, detail: 'stored balance differs' }],
      }),
    });
    renderWithProviders(<ReconciliationPage />, { client, session: admin });
    await userEvent.click(screen.getByRole('button', { name: 'Run reconciliation' }));
    expect(await screen.findByText('BALANCE_MISMATCH')).toBeInTheDocument();
    expect(screen.getByText('50000')).toBeInTheDocument();
  });
});

describe('StatisticsPage (FR-ADM-07)', () => {
  it('shows counts, totals by type and the latest reconciliation', async () => {
    const stats: PlatformStatistics = {
      users: 2,
      merchants: 1,
      walletsByStatus: { ACTIVE: 3 },
      transactionsByStatus: { COMPLETED: 4, FAILED: 1 },
      completedTotals: [{ type: 'TEST_FUNDING', currency: 'INR', count: 2, totalMinor: 700000, totalFormatted: '' }],
      latestReconciliation: { runId: 'R-1', runAt: '2026-11-28T10:00:00Z', result: 'PASS', discrepancyCount: 0 },
    };
    const { client } = fakeClient({ 'GET /admin/statistics': stats });
    renderWithProviders(<StatisticsPage />, { client, session: admin });
    expect(await screen.findByText('₹7,000.00')).toBeInTheDocument();
    expect(screen.getByText('Transactions FAILED')).toBeInTheDocument();
    expect(screen.getByText('PASS')).toBeInTheDocument();
  });
});
