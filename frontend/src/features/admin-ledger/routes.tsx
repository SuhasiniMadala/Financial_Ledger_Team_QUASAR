import type { FeatureRoutes } from '../types';
import { AccountEntriesPage } from './AccountEntriesPage';
import { ReconciliationPage } from './ReconciliationPage';
import { StatisticsPage } from './StatisticsPage';
import { TestFundingPage } from './TestFundingPage';
import { TransactionDetailPage } from './TransactionDetailPage';
import { TransactionsPage } from './TransactionsPage';

// Owner: Geethika M.
export const featureRoutes: FeatureRoutes = {
  role: 'ADMIN',
  nav: [
    { path: '/admin/transactions', label: 'Transactions & Ledger', order: 30 },
    { path: '/admin/reconciliation', label: 'Reconciliation', order: 40 },
    { path: '/admin/statistics', label: 'Statistics', order: 50 },
  ],
  routes: [
    { path: '/admin/wallets/:walletId/test-funding', element: <TestFundingPage /> },
    { path: '/admin/transactions', element: <TransactionsPage /> },
    { path: '/admin/transactions/:id', element: <TransactionDetailPage /> },
    { path: '/admin/accounts/:id/entries', element: <AccountEntriesPage /> },
    { path: '/admin/reconciliation', element: <ReconciliationPage /> },
    { path: '/admin/statistics', element: <StatisticsPage /> },
  ],
};
