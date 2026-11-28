import type { ApiClient } from '../../api/client';
import type { LedgerEntry, PageResponse, TransactionReceipt, TransactionStatus, TransactionType } from '../../api/types';

export interface AdminTransaction {
  transactionId: string;
  type: TransactionType;
  status: TransactionStatus;
  amountMinor: number;
  amountFormatted: string;
  currency: string;
  initiatedBy: string;
  sourceAccountId?: string;
  destinationAccountId?: string;
  reversalOfId?: string;
  reversedBy?: string;
  failureCode?: string;
  description?: string;
  createdAt: string;
}

export interface AdminTransactionDetail {
  transaction: AdminTransaction;
  entries: LedgerEntry[];
}

export interface TransactionFilters {
  type?: string;
  status?: string;
  walletId?: string;
  from?: string;
  to?: string;
}

export interface ReconciliationReport {
  runId: string;
  runAt: string;
  durationMs: number;
  result: 'PASS' | 'FAIL';
  currencies: {
    currency: string;
    totalDebitsMinor: number;
    totalCreditsMinor: number;
    totalDebitsFormatted: string;
    totalCreditsFormatted: string;
  }[];
  checked: { accounts: number; transactions: number; entries: number };
  discrepancies: {
    type: string;
    subjectId: string;
    expectedMinor: number;
    actualMinor: number;
    differenceMinor: number;
    detail?: string;
  }[];
}

export interface PlatformStatistics {
  users: number;
  merchants: number;
  walletsByStatus: Record<string, number>;
  transactionsByStatus: Record<string, number>;
  completedTotals: { type: string; currency: string; count: number; totalMinor: number; totalFormatted: string }[];
  latestReconciliation?: { runId: string; runAt: string; result: string; discrepancyCount: number };
}

/** FR-WAL-07/08: debit System Funding, credit the wallet. Idempotent (needs a key). */
export const testFund = (
  client: ApiClient,
  walletId: string,
  body: { amountMinor: number; description?: string },
  key: string,
) => client.postFinancial<TransactionReceipt>(`/admin/wallets/${encodeURIComponent(walletId)}/test-funding`, body, key);

/** FR-LED-07..10: mirror-image entries linked to the original. Reason 10-500 characters. Idempotent. */
export const reverseTransaction = (client: ApiClient, transactionId: string, body: { reason: string }, key: string) =>
  client.postFinancial<TransactionReceipt>(`/admin/transactions/${encodeURIComponent(transactionId)}/reversal`, body, key);

/** FR-ADM-03 */
export const listTransactions = (client: ApiClient, filters: TransactionFilters, page: number) =>
  client.get<PageResponse<AdminTransaction>>('/admin/transactions', { ...filters, page, size: 20 });

/** FR-ADM-03: a transaction with all of its ledger entries. */
export const getTransaction = (client: ApiClient, transactionId: string) =>
  client.get<AdminTransactionDetail>(`/admin/transactions/${encodeURIComponent(transactionId)}`);

/** FR-ADM-03: the ledger entries of one account, in sequence order. */
export const listAccountEntries = (client: ApiClient, accountId: string, page: number) =>
  client.get<PageResponse<LedgerEntry>>(`/admin/accounts/${encodeURIComponent(accountId)}/entries`, { page, size: 20 });

/** FR-REC-01..08: read-only, one snapshot, never repairs anything. No body, no idempotency key. */
export const runReconciliation = (client: ApiClient) => client.post<ReconciliationReport>('/admin/reconciliations');

/** FR-ADM-07 */
export const getStatistics = (client: ApiClient) => client.get<PlatformStatistics>('/admin/statistics');

/** Total debits and credits of a set of entries, in integer paise (FR-LED-02 shown on screen). */
export function entryTotals(entries: LedgerEntry[]): { debits: number; credits: number } {
  return entries.reduce(
    (sum, e) => (e.direction === 'DEBIT' ? { ...sum, debits: sum.debits + e.amountMinor } : { ...sum, credits: sum.credits + e.amountMinor }),
    { debits: 0, credits: 0 },
  );
}
