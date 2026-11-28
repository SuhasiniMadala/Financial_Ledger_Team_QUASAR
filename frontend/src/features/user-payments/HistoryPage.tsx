import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { TRANSACTION_STATUSES, TRANSACTION_TYPES } from '../../api/types';
import { DateRangeFilter } from '../../components/DateRangeFilter';
import { DataState, StatusBadge } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { Pagination } from '../../components/Pagination';
import { useApiQuery } from '../../hooks/useApiQuery';
import { formatTimestamp } from '../../lib/dates';
import { listWallets } from '../user-wallets/api';
import { listHistory, type HistoryFilters } from './api';

const EMPTY: HistoryFilters = { walletId: '', type: '', status: '', from: '', to: '' };

/** USER History (SRS 3.1, FR-LED-11): own transactions and own FAILED attempts, newest first, with filters. */
export function HistoryPage() {
  const wallets = useApiQuery('wallets', listWallets);
  const [draft, setDraft] = useState<HistoryFilters>(EMPTY);
  const [applied, setApplied] = useState<HistoryFilters>(EMPTY);
  const [page, setPage] = useState(0);
  const history = useApiQuery(`history:${JSON.stringify(applied)}:${page}`, (c) => listHistory(c, applied, page));

  const apply = (event: FormEvent) => {
    event.preventDefault();
    setApplied(draft);
    setPage(0);
  };

  return (
    <>
      <h1>Transaction history</h1>
      <form className="card filters" onSubmit={apply}>
        <label>
          Wallet
          <select value={draft.walletId} onChange={(e) => setDraft({ ...draft, walletId: e.target.value })}>
            <option value="">All my wallets</option>
            {(wallets.data ?? []).map((w) => (
              <option key={w.walletId} value={w.walletId}>
                {w.walletId} ({w.currency})
              </option>
            ))}
          </select>
        </label>
        <label>
          Type
          <select value={draft.type} onChange={(e) => setDraft({ ...draft, type: e.target.value })}>
            <option value="">All</option>
            {TRANSACTION_TYPES.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </label>
        <label>
          Status
          <select value={draft.status} onChange={(e) => setDraft({ ...draft, status: e.target.value })}>
            <option value="">All</option>
            {TRANSACTION_STATUSES.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </label>
        <DateRangeFilter from={draft.from ?? ''} to={draft.to ?? ''} onChange={(r) => setDraft({ ...draft, ...r })} />
        <button type="submit">Apply filters</button>
      </form>
      <section className="card">
        <DataState loading={history.loading} error={history.error} data={history.data}>
          {(data) =>
            data.items.length === 0 ? (
              <p className="muted">No transactions yet.</p>
            ) : (
              <>
                <table>
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>Transaction</th>
                      <th>Type</th>
                      <th>Status</th>
                      <th>Counterparty</th>
                      <th className="num">Amount</th>
                      <th>Notes</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.items.map((t) => (
                      <tr key={`${t.transactionId}-${t.walletId}`}>
                        <td>{formatTimestamp(t.createdAt)}</td>
                        <td>
                          <Link to={`/user/history/${t.transactionId}`}>{t.transactionId}</Link>
                        </td>
                        <td>{t.type}</td>
                        <td>
                          <StatusBadge value={t.status} />
                        </td>
                        <td>{t.counterpartyName ?? ''}</td>
                        <td className="num">
                          {t.direction === 'OUTGOING' ? '- ' : '+ '}
                          <Money minor={t.amountMinor} currency={t.currency} />
                        </td>
                        <td>
                          {t.failureCode && <span>{t.failureCode} </span>}
                          {t.reversedBy && <span>reversed by {t.reversedBy} </span>}
                          {t.description && <span>{t.description}</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onPage={setPage} />
              </>
            )
          }
        </DataState>
      </section>
    </>
  );
}
