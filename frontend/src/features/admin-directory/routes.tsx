import type { FeatureRoutes } from '../types';
import { DirectoryPage } from './DirectoryPage';
import { WalletsPage } from './WalletsPage';

// Owner: Akshaya A.
export const featureRoutes: FeatureRoutes = {
  role: 'ADMIN',
  nav: [
    { path: '/admin/directory', label: 'Users & Merchants', order: 10 },
    { path: '/admin/wallets', label: 'Wallets', order: 20 },
  ],
  routes: [
    { path: '/admin/directory', element: <DirectoryPage /> },
    { path: '/admin/wallets', element: <WalletsPage /> },
  ],
};
