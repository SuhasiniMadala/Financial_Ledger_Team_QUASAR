// Types shared by several features. They mirror the backend DTOs exactly (docs/api/openapi.json).
// Money is always an integer number of minor units (paise for INR) in fields ending in "Minor" (BR-01, TQ-6).
// Fields the backend may omit (null values are not serialised) are optional here.

export type Role = 'USER' | 'MERCHANT' | 'ADMIN';
export type TransactionType = 'TEST_FUNDING' | 'TRANSFER' | 'MERCHANT_PAYMENT' | 'REVERSAL';
export type TransactionStatus = 'COMPLETED' | 'FAILED';
export type WalletStatus = 'ACTIVE' | 'FROZEN' | 'DEACTIVATED';
export type EntryDirection = 'DEBIT' | 'CREDIT';

export const TRANSACTION_TYPES: TransactionType[] = ['TEST_FUNDING', 'TRANSFER', 'MERCHANT_PAYMENT', 'REVERSAL'];
export const TRANSACTION_STATUSES: TransactionStatus[] = ['COMPLETED', 'FAILED'];

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  accountId: string;
  role: Role;
  displayName: string;
}

export interface Wallet {
  walletId: string;
  currency: string;
  balanceMinor: number;
  balanceFormatted: string;
  status: WalletStatus;
  createdAt: string;
}

/** 201 body of every financial POST (transfer, payment, test funding, reversal). */
export interface TransactionReceipt {
  transactionId: string;
  type: TransactionType;
  status: TransactionStatus;
  amountMinor: number;
  amountFormatted: string;
  currency: string;
  sourceAccountId?: string;
  destinationAccountId?: string;
  reversalOfId?: string;
  description?: string;
  createdAt: string;
}

export interface LedgerEntry {
  entryId: string;
  sequenceNo: number;
  transactionId: string;
  accountId: string;
  direction: EntryDirection;
  amountMinor: number;
  balanceAfterMinor: number;
  createdAt: string;
}

/** ID formats (backend common/id/Ids.java). Used for client-side checks before sending. */
export const WALLET_ID_PATTERN = /^W-[0-9A-Z]{10}$/;
export const MERCHANT_CODE_PATTERN = /^MER-[0-9A-Z]{6}$/;
