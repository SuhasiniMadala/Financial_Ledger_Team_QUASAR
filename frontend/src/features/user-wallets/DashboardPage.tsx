import { useState, type FormEvent } from 'react';
import { toApiError, type ApiError } from '../../api/errors';
import type { Wallet } from '../../api/types';
import { useApiClient, useSession } from '../../auth/AuthContext';
import { DataState, ErrorBanner, StatusBadge, SuccessBanner } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { useApiQuery } from '../../hooks/useApiQuery';
import { createWallet, listWallets, SUPPORTED_CURRENCIES } from './api';

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  if (!navigator.clipboard) {
    return null;
  }
  return (
    <button
      type="button"
      className="secondary"
      onClick={() => navigator.clipboard.writeText(text).then(() => setCopied(true))}
      aria-label={`Copy ${text}`}
    >
      {copied ? 'Copied' : 'Copy ID'}
    </button>
  );
}

/** USER Dashboard (SRS 3.1): wallets and balances, create wallet. FR-WAL-01, FR-WAL-03, FR-WAL-04, FR-WAL-06. */
export function DashboardPage() {
  const client = useApiClient();
  const session = useSession();
  const wallets = useApiQuery('wallets', listWallets);
  const [currency, setCurrency] = useState('INR');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<ApiError>();
  const [created, setCreated] = useState<Wallet>();

  const create = async (event: FormEvent) => {
    event.preventDefault();
    setCreating(true);
    setCreateError(undefined);
    setCreated(undefined);
    try {
      setCreated(await createWallet(client, currency));
      wallets.reload();
    } catch (caught) {
      setCreateError(toApiError(caught));
    } finally {
      setCreating(false);
    }
  };

  return (
    <>
      <h1>Welcome, {session.displayName}</h1>
      <section className="card">
        <h2>My wallets</h2>
        <DataState loading={wallets.loading} error={wallets.error} data={wallets.data}>
          {(list) =>
            list.length === 0 ? (
              <p className="muted">You have no wallets yet. Create one below.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Wallet ID</th>
                    <th>Currency</th>
                    <th className="num">Balance</th>
                    <th>Balance (paise)</th>
                    <th>Status</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {list.map((w) => (
                    <tr key={w.walletId}>
                      <td>
                        <code>{w.walletId}</code>
                      </td>
                      <td>{w.currency}</td>
                      <td className="num">
                        <Money minor={w.balanceMinor} currency={w.currency} />
                      </td>
                      <td>{w.balanceMinor}</td>
                      <td>
                        <StatusBadge value={w.status} />
                      </td>
                      <td>
                        <CopyButton text={w.walletId} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )
          }
        </DataState>
        <p className="muted">
          To receive money, give the sender your wallet ID. Money is added only by an admin test funding or by transfers.
        </p>
      </section>
      <form className="card form" onSubmit={create}>
        <h2>Create a wallet</h2>
        <label>
          Currency
          <select value={currency} onChange={(e) => setCurrency(e.target.value)}>
            {SUPPORTED_CURRENCIES.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </label>
        {created && <SuccessBanner>Wallet {created.walletId} created with balance ₹0.00.</SuccessBanner>}
        <ErrorBanner error={createError} />
        <div className="actions">
          <button type="submit" disabled={creating}>
            {creating ? 'Creating...' : 'Create wallet'}
          </button>
        </div>
      </form>
    </>
  );
}
