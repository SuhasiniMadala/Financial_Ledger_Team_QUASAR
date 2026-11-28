import type { Role } from '../api/types';
import { featureRoutes as adminDirectory } from './admin-directory/routes';
import { featureRoutes as adminLedger } from './admin-ledger/routes';
import { featureRoutes as merchant } from './merchant/routes';
import type { FeatureRoutes, NavItem } from './types';
import { featureRoutes as userPayments } from './user-payments/routes';
import { featureRoutes as userWallets } from './user-wallets/routes';

// Owners: user-wallets + admin-directory = Akshaya; admin-ledger = Geethika; user-payments + merchant = Suhasini.
export const FEATURES: FeatureRoutes[] = [userWallets, userPayments, merchant, adminDirectory, adminLedger];

export function navFor(role: Role): NavItem[] {
  return FEATURES.filter((f) => f.role === role)
    .flatMap((f) => f.nav)
    .sort((a, b) => a.order - b.order);
}

export function routesFor(role: Role): FeatureRoutes['routes'] {
  return FEATURES.filter((f) => f.role === role).flatMap((f) => f.routes);
}
