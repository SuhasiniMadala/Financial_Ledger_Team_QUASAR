import type { ApiClient } from '../../api/client';
import type { PageResponse, Wallet, WalletStatus } from '../../api/types';

export interface UserSummary {
  accountId: string;
  name: string;
  email: string;
  registeredAt: string;
  merchantCode?: string;
}

export type AccountType = 'USER_WALLET' | 'MERCHANT_WALLET' | 'SYSTEM_FUNDING';

export interface AccountSummary {
  accountId: string;
  ownerId?: string;
  ownerName?: string;
  type: AccountType;
  currency: string;
  balanceMinor: number;
  balanceFormatted: string;
  status: WalletStatus;
  createdAt: string;
}

export type StatusAction = 'freeze' | 'unfreeze' | 'deactivate';

/** FR-ADM-04: the only allowed changes. The backend enforces them too (409 INVALID_STATE_TRANSITION). */
export const ALLOWED_ACTIONS: Record<WalletStatus, StatusAction[]> = {
  ACTIVE: ['freeze', 'deactivate'],
  FROZEN: ['unfreeze', 'deactivate'],
  DEACTIVATED: [],
};

/** FR-ADM-01 */
export const listUsers = (client: ApiClient, page: number) =>
  client.get<PageResponse<UserSummary>>('/admin/users', { page, size: 20 });

/** FR-ADM-01 */
export const listMerchants = (client: ApiClient, page: number) =>
  client.get<PageResponse<UserSummary>>('/admin/merchants', { page, size: 20 });

/** FR-ADM-02: includes the System Funding accounts. */
export const listAccounts = (client: ApiClient, page: number) =>
  client.get<PageResponse<AccountSummary>>('/admin/accounts', { page, size: 20 });

/** FR-ADM-04/05: a reason is required and recorded with the admin, time, old and new status. */
export const changeWalletStatus = (client: ApiClient, walletId: string, action: StatusAction, reason: string) =>
  client.post<Wallet>(`/admin/wallets/${encodeURIComponent(walletId)}/${action}`, { reason });
