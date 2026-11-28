// End-to-end check that the client's API modules and the real backend agree (FR-1..FR-5 through the client code).
// No mocks. Money is checked in stored balances, not only HTTP statuses, and the run ends with reconciliation PASS
// (AGENTS.md testing rules). Requires a running backend with an admin configured; see README.md in this folder.
import { beforeAll, describe, expect, it } from 'vitest';
import { createApiClient, type ApiClient } from '../../src/api/client';
import { ApiError } from '../../src/api/errors';
import type { LoginResponse } from '../../src/api/types';
import { newIdempotencyKey } from '../../src/hooks/useFinancialSubmit';
import { getTransaction, reverseTransaction, runReconciliation, testFund } from '../../src/features/admin-ledger/api';
import { getMerchantAccount, getMerchantStatistics } from '../../src/features/merchant/api';
import { getHistoryItem, listHistory, payMerchant, sendTransfer } from '../../src/features/user-payments/api';
import { createWallet, listWallets } from '../../src/features/user-wallets/api';

const BASE_URL = process.env.LEDGER_API_URL ?? 'http://localhost:8080';
const ADMIN_EMAIL = process.env.LEDGER_ADMIN_EMAIL;
const ADMIN_PASSWORD = process.env.LEDGER_ADMIN_PASSWORD;
const PASSWORD = 'integration-test-password';
const run = Date.now().toString(36);

async function login(email: string, password: string): Promise<{ client: ApiClient; login: LoginResponse }> {
  const anonymous = createApiClient({ baseUrl: BASE_URL });
  const response = await anonymous.post<LoginResponse>('/auth/login', { email, password });
  return { client: createApiClient({ baseUrl: BASE_URL, getToken: () => response.accessToken }), login: response };
}

async function register(kind: 'user' | 'merchant', name: string) {
  const email = `${kind}-${name.toLowerCase().replace(/\W/g, '')}-${run}@example.test`;
  const anonymous = createApiClient({ baseUrl: BASE_URL });
  const body = kind === 'user' ? { displayName: name, email, password: PASSWORD } : { businessName: name, email, password: PASSWORD };
  const registration = await anonymous.post<{ merchantCode?: string }>(kind === 'user' ? '/auth/register' : '/auth/register/merchant', body);
  return { ...(await login(email, PASSWORD)), merchantCode: registration.merchantCode };
}

async function balanceOf(client: ApiClient, walletId: string): Promise<number> {
  const wallet = (await listWallets(client)).find((w) => w.walletId === walletId);
  if (!wallet) throw new Error(`wallet ${walletId} not found`);
  return wallet.balanceMinor;
}

// Locally the suite is skipped without credentials; in CI it must run, so a missing variable fails the job.
const IN_CI = process.env.CI === 'true';
it.runIf(IN_CI)('CI provides the admin credentials, so the suite below really runs', () => {
  expect(ADMIN_EMAIL && ADMIN_PASSWORD).toBeTruthy();
});

describe.skipIf(!ADMIN_EMAIL || !ADMIN_PASSWORD)('client <-> real backend', () => {
  let admin: ApiClient;
  let asha: Awaited<ReturnType<typeof register>>;
  let ravi: Awaited<ReturnType<typeof register>>;
  let cafe: Awaited<ReturnType<typeof register>>;
  let ashaWallet: string;
  let raviWallet: string;

  beforeAll(async () => {
    const health = await createApiClient({ baseUrl: BASE_URL }).get<{ status: string }>('/health');
    expect(health.status).toBe('UP');
    admin = (await login(ADMIN_EMAIL as string, ADMIN_PASSWORD as string)).client;
    asha = await register('user', 'Asha Rao');
    ravi = await register('user', 'Ravi Kumar');
    cafe = await register('merchant', 'Campus Cafe');
    ashaWallet = (await createWallet(asha.client, 'INR')).walletId;
    raviWallet = (await createWallet(ravi.client, 'INR')).walletId;
  });

  it('login returns the display name for the top bar (SRS 3.1)', () => {
    expect(asha.login.displayName).toBe('Asha Rao');
    expect(cafe.login.displayName).toBe('Campus Cafe');
    expect(cafe.login.role).toBe('MERCHANT');
  });

  it('test funding is idempotent: a retry with the same key is replayed, not repeated (FR-WAL-07/08)', async () => {
    const key = newIdempotencyKey();
    const first = await testFund(admin, ashaWallet, { amountMinor: 500000 }, key);
    const retry = await testFund(admin, ashaWallet, { amountMinor: 500000 }, key);
    expect(retry.replayed).toBe(true);
    expect(retry.data.transactionId).toBe(first.data.transactionId);
    expect(await balanceOf(asha.client, ashaWallet)).toBe(500000);
  });

  it('transfer moves exactly the amount, and a reversal restores it (FR-TRF-14, FR-LED-07)', async () => {
    const transfer = await sendTransfer(asha.client, { sourceWalletId: ashaWallet, destinationWalletId: raviWallet, amountMinor: 100000 }, newIdempotencyKey());
    expect(transfer.data.status).toBe('COMPLETED');
    expect(await balanceOf(asha.client, ashaWallet)).toBe(400000);
    expect(await balanceOf(ravi.client, raviWallet)).toBe(100000);

    const detail = await getTransaction(admin, transfer.data.transactionId);
    const debits = detail.entries.filter((e) => e.direction === 'DEBIT').reduce((s, e) => s + e.amountMinor, 0);
    const credits = detail.entries.filter((e) => e.direction === 'CREDIT').reduce((s, e) => s + e.amountMinor, 0);
    expect(debits).toBe(credits);

    const userView = await getHistoryItem(asha.client, transfer.data.transactionId);
    expect(userView.transaction.counterpartyName).toBe('Ravi Kumar');
    expect(userView.ownEntries.every((e) => e.accountId === ashaWallet)).toBe(true);

    const reversal = await reverseTransaction(admin, transfer.data.transactionId, { reason: 'integration test: wrong recipient' }, newIdempotencyKey());
    expect(reversal.data.reversalOfId).toBe(transfer.data.transactionId);
    expect(await balanceOf(asha.client, ashaWallet)).toBe(500000);
    expect(await balanceOf(ravi.client, raviWallet)).toBe(0);
  });

  it('insufficient funds is a recorded FAILED transaction and moves no money (FR-TRF-11/13)', async () => {
    const error = await sendTransfer(asha.client, { sourceWalletId: ashaWallet, destinationWalletId: raviWallet, amountMinor: 600000 }, newIdempotencyKey()).catch(
      (e: unknown) => e,
    );
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 422, code: 'INSUFFICIENT_FUNDS' });
    const history = await listHistory(asha.client, { status: 'FAILED' }, 0);
    expect(history.items.map((t) => t.transactionId)).toContain((error as ApiError).transactionId);
    expect(await balanceOf(asha.client, ashaWallet)).toBe(500000);
  });

  it('merchant payment reaches the merchant, who sees it in account and statistics (FR-PAY-01..07)', async () => {
    await payMerchant(asha.client, { sourceWalletId: ashaWallet, merchantCode: cafe.merchantCode as string, amountMinor: 25000, reference: 'Order 1187' }, newIdempotencyKey());
    expect(await balanceOf(asha.client, ashaWallet)).toBe(475000);
    expect((await getMerchantAccount(cafe.client)).balanceMinor).toBe(25000);
    const stats = await getMerchantStatistics(cafe.client, {});
    expect(stats).toMatchObject({ paymentCount: 1, totalMinor: 25000, averageMinor: 25000 });
  });

  it('ends with reconciliation PASS (FR-REC-08, NFR-SAFE-02)', async () => {
    const report = await runReconciliation(admin);
    expect(report.discrepancies).toEqual([]);
    expect(report.result).toBe('PASS');
  });
});
