import type { FeatureRoutes } from '../types';
import { HistoryDetailPage } from './HistoryDetailPage';
import { HistoryPage } from './HistoryPage';
import { PayMerchantPage } from './PayMerchantPage';
import { TransferPage } from './TransferPage';

// Owner: Suhasini M.
export const featureRoutes: FeatureRoutes = {
  role: 'USER',
  nav: [
    { path: '/user/transfer', label: 'Transfer', order: 20 },
    { path: '/user/pay', label: 'Pay Merchant', order: 30 },
    { path: '/user/history', label: 'History', order: 40 },
  ],
  routes: [
    { path: '/user/transfer', element: <TransferPage /> },
    { path: '/user/pay', element: <PayMerchantPage /> },
    { path: '/user/history', element: <HistoryPage /> },
    { path: '/user/history/:id', element: <HistoryDetailPage /> },
  ],
};
