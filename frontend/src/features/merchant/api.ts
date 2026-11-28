import type { ApiClient } from '../../api/client';
import type { PageResponse, TransactionType, WalletStatus } from '../../api/types';

export interface MerchantAccount {
  businessName: string;
  merchantCode: string;
  walletId: string;
  currency: string;
  balanceMinor: number;
  balanceFormatted: string;
  status: WalletStatus;
}

/** No payer wallet ID or balance, by design (FR-PAY-06). */
export interface MerchantTransaction {
  transactionId: string;
  type: TransactionType;
  direction: 'INCOMING' | 'OUTGOING';
  amountMinor: number;
  amountFormatted: string;
  currency: string;
  payerName?: string;
  reference?: string;
  reversalOfId?: string;
  createdAt: string;
}

export interface MerchantStatistics {
  from: string;
  to: string;
  currency: string;
  paymentCount: number;
  totalMinor: number;
  totalFormatted: string;
  /** Rounded half-even to the nearest paisa by the backend (FR-PAY-07). */
  averageMinor: number;
  averageFormatted: string;
}

export interface DateRange {
  from?: string;
  to?: string;
}

/** FR-PAY-04 */
export const getMerchantAccount = (client: ApiClient) => client.get<MerchantAccount>('/merchant/account');

/** FR-PAY-05: payments and reversals on the merchant's wallet, newest first, filtered by date. */
export const listMerchantTransactions = (client: ApiClient, range: DateRange, page: number) =>
  client.get<PageResponse<MerchantTransaction>>('/merchant/transactions', { ...range, page, size: 20 });

/** FR-PAY-07: default range is the last 30 days. */
export const getMerchantStatistics = (client: ApiClient, range: DateRange) =>
  client.get<MerchantStatistics>('/merchant/statistics', { ...range });
