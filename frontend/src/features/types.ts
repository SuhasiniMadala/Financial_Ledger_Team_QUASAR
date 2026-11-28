import type { ReactElement } from 'react';
import type { Role } from '../api/types';

export interface NavItem {
  path: string;
  label: string;
  /** Position in the role's navigation bar (SRS 3.1 order). */
  order: number;
}

/**
 * Each feature folder exports one of these from its routes.tsx. App.tsx and the navigation bar read them through
 * features/registry.ts, so adding a screen never means editing a file owned by another team member.
 */
export interface FeatureRoutes {
  role: Role;
  nav: NavItem[];
  routes: { path: string; element: ReactElement }[];
}
