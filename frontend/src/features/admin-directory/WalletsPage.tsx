import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { toApiError, type ApiError } from '../../api/errors';
import { useApiClient } from '../../auth/AuthContext';
import { DataState, ErrorBanner, StatusBadge, SuccessBanner } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { Pagination } from '../../components/Pagination';
import { useApiQuery } from '../../hooks/useApiQuery';
import { ALLOWED_ACTIONS, changeWalletStatus, listAccounts, type AccountSummary, type StatusAction } from './api';

const ACTION_LABEL: Record<StatusAction, string> = { freeze: 'Freeze', unfreeze: 'Unfreeze', deactivate: 'Deactivate' };

/**
 * ADMIN Wallets (SRS 3.1): every account with owner, type, currency, balance and status (FR-ADM-02), status actions
 * with a required reason (FR-ADM-04/05) and a link to test funding (Geethika's screen). Status changes never touch
 * balances or the ledger (FR-ADM-06).
 */
export function WalletsPage() {
  const client = useApiClient();
  const [page, setPage] = useState(0);
  const accounts = useApiQuery(`accounts:${page}`, (c) => listAccounts(c, page));
  const [pending, setPending] = useState<{ account: AccountSummary; action: StatusAction }>();
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<ApiError>();
  const [message, setMessage] = useState<string>();

  const start = (account: AccountSummary, action: StatusAction) => {
    setPending({ account, action });
    setReason('');
    setError(undefined);
    setMessage(undefined);
  };

  const confirm = async (event: FormEvent) => {
    event.preventDefault();
    if (!pending || !reason.trim()) {
      return;
    }
    setBusy(true);
    setError(undefined);
    try {
      const wallet = await changeWalletStatus(client, pending.account.accountId, pending.action, reason.trim());
      setMessage(`${wallet.walletId} is now ${wallet.status}.`);
      setPending(undefined);
      accounts.reload();
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <h1>Wallets and accounts</h1>
      {message && <SuccessBanner>{message}</SuccessBanner>}
      {pending && (
        <form className="card form" onSubmit={confirm}>
          <h2>
            {ACTION_LABEL[pending.action]} {pending.account.accountId}
          </h2>
          {pending.action === 'deactivate' && (
            <p className="banner error">Deactivation is final. The wallet can never take part in a transaction again.</p>
          )}
          <label>
            Reason (required, recorded in the audit trail)
            <textarea value={reason} maxLength={500} rows={2} onChange={(e) => setReason(e.target.value)} />
          </label>
          <ErrorBanner error={error} />
          <div className="actions">
            <button type="submit" className={pending.action === 'deactivate' ? 'danger' : undefined} disabled={busy || !reason.trim()}>
              {busy ? 'Saving...' : `Confirm ${ACTION_LABEL[pending.action].toLowerCase()}`}
            </button>
            <button type="button" className="secondary" onClick={() => setPending(undefined)} disabled={busy}>
              Cancel
            </button>
          </div>
        </form>
      )}
      <section className="card">
        <DataState loading={accounts.loading} error={accounts.error} data={accounts.data}>
          {(data) => (
            <>
              <table>
                <thead>
                  <tr>
                    <th>Account</th>
                    <th>Owner</th>
                    <th>Type</th>
                    <th className="num">Balance</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((a) => (
                    <tr key={a.accountId}>
                      <td>
                        <code>{a.accountId}</code>
                      </td>
                      <td>{a.ownerName ?? 'Platform (system)'}</td>
                      <td>{a.type}</td>
                      <td className="num">
                        <Money minor={a.balanceMinor} currency={a.currency} />
                      </td>
                      <td>
                        <StatusBadge value={a.status} />
                      </td>
                      <td className="actions">
                        {a.type !== 'SYSTEM_FUNDING' &&
                          ALLOWED_ACTIONS[a.status].map((action) => (
                            <button key={action} type="button" className="secondary" onClick={() => start(a, action)}>
                              {ACTION_LABEL[action]}
                            </button>
                          ))}
                        {a.type === 'USER_WALLET' && a.status === 'ACTIVE' && (
                          <Link to={`/admin/wallets/${a.accountId}/test-funding`}>Test funding</Link>
                        )}
                        <Link to={`/admin/accounts/${a.accountId}/entries?currency=${a.currency}`}>Entries</Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onPage={setPage} />
            </>
          )}
        </DataState>
      </section>
    </>
  );
}
