import type { ApiClient } from '../../api/client';
import type { LedgerEntry, PageResponse, TransactionReceipt, TransactionStatus, TransactionType } from '../../api/types';

export interface TransferBody {
  sourceWalletId: string;
  destinationWalletId: string;
  amountMinor: number;
  description?: string;
}

export interface PaymentBody {
  sourceWalletId: string;
  merchantCode: string;
  amountMinor: number;
  reference?: string;
}

/** A transaction as the USER sees it: their own side only (FR-LED-11/12). */
export interface UserTransaction {
  transactionId: string;
  type: TransactionType;
  status: TransactionStatus;
  direction: 'OUTGOING' | 'INCOMING';
  walletId: string;
  amountMinor: number;
  amountFormatted: string;
  currency: string;
  counterpartyName?: string;
  description?: string;
  failureCode?: string;
  reversalOfId?: string;
  reversedBy?: string;
  createdAt: string;
}

export interface UserTransactionDetail {
  transaction: UserTransaction;
  /** Only the caller's own entries; never the other party's entries or balance (FR-LED-12). */
  ownEntries: LedgerEntry[];
}

export interface HistoryFilters {
  walletId?: string;
  type?: string;
  status?: string;
  from?: string;
  to?: string;
}

/** FR-TRF-01..14: USER wallet to USER wallet, same currency. Idempotent (needs a key). */
export const sendTransfer = (client: ApiClient, body: TransferBody, key: string) =>
  client.postFinancial<TransactionReceipt>('/transfers', body, key);

/** FR-PAY-01..03: USER wallet to the merchant's wallet, identified by merchant code. Idempotent. */
export const payMerchant = (client: ApiClient, body: PaymentBody, key: string) =>
  client.postFinancial<TransactionReceipt>('/payments', body, key);

/** FR-LED-11: own transactions and own FAILED attempts, newest first. */
export const listHistory = (client: ApiClient, filters: HistoryFilters, page: number) =>
  client.get<PageResponse<UserTransaction>>('/transactions', { ...filters, page, size: 20 });

/** FR-LED-12 */
export const getHistoryItem = (client: ApiClient, transactionId: string) =>
  client.get<UserTransactionDetail>(`/transactions/${encodeURIComponent(transactionId)}`);
