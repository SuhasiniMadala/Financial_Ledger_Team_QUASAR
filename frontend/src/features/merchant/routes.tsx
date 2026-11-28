import type { FeatureRoutes } from '../types';
import { AccountPage } from './AccountPage';
import { PaymentsPage } from './PaymentsPage';
import { MerchantStatisticsPage } from './StatisticsPage';

// Owner: Suhasini M.
export const featureRoutes: FeatureRoutes = {
  role: 'MERCHANT',
  nav: [
    { path: '/merchant/account', label: 'Account', order: 10 },
    { path: '/merchant/payments', label: 'Payments', order: 20 },
    { path: '/merchant/statistics', label: 'Statistics', order: 30 },
  ],
  routes: [
    { path: '/merchant/account', element: <AccountPage /> },
    { path: '/merchant/payments', element: <PaymentsPage /> },
    { path: '/merchant/statistics', element: <MerchantStatisticsPage /> },
  ],
};
