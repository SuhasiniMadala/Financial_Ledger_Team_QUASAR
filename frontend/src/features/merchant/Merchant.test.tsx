import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { fakeClient } from '../../test/fakeClient';
import { renderWithProviders, testSession } from '../../test/renderWithProviders';
import { navFor } from '../registry';
import { AccountPage } from './AccountPage';
import { PaymentsPage } from './PaymentsPage';
import { MerchantStatisticsPage } from './StatisticsPage';

const merchant = testSession('MERCHANT', 'Campus Cafe');

describe('MERCHANT navigation (SRS 3.1)', () => {
  it('has Account, Payments and Statistics', () => {
    expect(navFor('MERCHANT').map((n) => n.label)).toEqual(['Account', 'Payments', 'Statistics']);
  });
});

describe('Merchant screens (FR-PAY-04..07)', () => {
  it('Account shows business name, code, wallet, balance and status (FR-PAY-04)', async () => {
    const { client } = fakeClient({
      'GET /merchant/account': { businessName: 'Campus Cafe', merchantCode: 'MER-7Q2K9D', walletId: 'W-2001000000', currency: 'INR', balanceMinor: 25000, balanceFormatted: '₹250.00', status: 'ACTIVE' },
    });
    renderWithProviders(<AccountPage />, { client, session: merchant });
    expect(await screen.findByText('MER-7Q2K9D')).toBeInTheDocument();
    expect(screen.getByText('₹250.00')).toBeInTheDocument();
  });

  it('Payments lists payer name and reference, refunds as reversals, and filters by date (FR-PAY-05/06)', async () => {
    const { client, calls } = fakeClient({
      'GET /merchant/transactions': {
        items: [
          { transactionId: 'T-504000000000', type: 'REVERSAL', direction: 'OUTGOING', amountMinor: 25000, amountFormatted: '', currency: 'INR', payerName: 'Asha Rao', reversalOfId: 'T-503000000000', createdAt: '2026-11-28T11:00:00Z' },
          { transactionId: 'T-503000000000', type: 'MERCHANT_PAYMENT', direction: 'INCOMING', amountMinor: 25000, amountFormatted: '', currency: 'INR', payerName: 'Asha Rao', reference: 'Order 1187', createdAt: '2026-11-28T10:00:00Z' },
        ],
        page: 0,
        size: 20,
        totalItems: 2,
        totalPages: 1,
      },
    });
    renderWithProviders(<PaymentsPage />, { client, session: merchant });
    expect(await screen.findByText('Order 1187')).toBeInTheDocument();
    expect(screen.getByText('Refund of T-503000000000')).toBeInTheDocument();
    expect(screen.queryByText(/W-1001/)).not.toBeInTheDocument(); // payer wallet never shown
    await userEvent.type(screen.getByLabelText('From'), '2026-11-01');
    await userEvent.click(screen.getByRole('button', { name: 'Apply' }));
    expect(calls.at(-1)?.query).toMatchObject({ from: '2026-11-01', page: 0 });
  });

  it('Statistics shows count, total and the backend-rounded average (FR-PAY-07)', async () => {
    const { client } = fakeClient({
      'GET /merchant/statistics': { from: '2026-10-30', to: '2026-11-28', currency: 'INR', paymentCount: 3, totalMinor: 100000, totalFormatted: '', averageMinor: 33333, averageFormatted: '' },
    });
    renderWithProviders(<MerchantStatisticsPage />, { client, session: merchant });
    expect(await screen.findByTestId('payment-count')).toHaveTextContent('3');
    expect(screen.getByText('₹1,000.00')).toBeInTheDocument();
    expect(screen.getByText('₹333.33')).toBeInTheDocument();
  });
});
