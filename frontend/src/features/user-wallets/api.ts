import type { ApiClient } from '../../api/client';
import type { Wallet } from '../../api/types';

/** Supported currencies (ledger.currencies, ADR-008). The backend rejects anything else with 400. */
export const SUPPORTED_CURRENCIES = ['INR', 'USD'];

/** FR-WAL-06: the caller's own wallets (a plain array, not paged). */
export const listWallets = (client: ApiClient) => client.get<Wallet[]>('/wallets');

/** FR-WAL-01/03/04: one wallet per currency, starting at zero. */
export const createWallet = (client: ApiClient, currency: string) => client.post<Wallet>('/wallets', { currency });
