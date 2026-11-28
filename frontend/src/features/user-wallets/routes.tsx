import type { FeatureRoutes } from '../types';
import { DashboardPage } from './DashboardPage';

// Owner: Akshaya A.
export const featureRoutes: FeatureRoutes = {
  role: 'USER',
  nav: [{ path: '/user/dashboard', label: 'Dashboard', order: 10 }],
  routes: [{ path: '/user/dashboard', element: <DashboardPage /> }],
};
