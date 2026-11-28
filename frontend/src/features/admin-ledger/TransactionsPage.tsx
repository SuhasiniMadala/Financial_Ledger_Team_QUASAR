import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { TRANSACTION_STATUSES, TRANSACTION_TYPES } from '../../api/types';
import { DateRangeFilter } from '../../components/DateRangeFilter';
import { DataState, StatusBadge } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { Pagination } from '../../components/Pagination';
import { useApiQuery } from '../../hooks/useApiQuery';
import { formatTimestamp } from '../../lib/dates';
import { listTransactions, type TransactionFilters } from './api';

const EMPTY: TransactionFilters = { type: '', status: '', walletId: '', from: '', to: '' };

/** ADMIN Transactions & Ledger (SRS 3.1, FR-ADM-03): every transaction, filtered by type, status, wallet and date. */
export function TransactionsPage() {
  const [draft, setDraft] = useState<TransactionFilters>(EMPTY);
  const [applied, setApplied] = useState<TransactionFilters>(EMPTY);
  const [page, setPage] = useState(0);
  const txs = useApiQuery(`admin-tx:${JSON.stringify(applied)}:${page}`, (c) => listTransactions(c, applied, page));

  const apply = (event: FormEvent) => {
    event.preventDefault();
    setApplied({ ...draft, walletId: draft.walletId?.trim() });
    setPage(0);
  };

  return (
    <>
      <h1>Transactions and ledger</h1>
      <form className="card filters" onSubmit={apply}>
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
        <label>
          Wallet ID
          <input value={draft.walletId} onChange={(e) => setDraft({ ...draft, walletId: e.target.value })} placeholder="W-..." />
        </label>
        <DateRangeFilter from={draft.from ?? ''} to={draft.to ?? ''} onChange={(r) => setDraft({ ...draft, ...r })} />
        <button type="submit">Apply filters</button>
      </form>
      <section className="card">
        <DataState loading={txs.loading} error={txs.error} data={txs.data}>
          {(data) =>
            data.items.length === 0 ? (
              <p className="muted">No transactions match.</p>
            ) : (
              <>
                <table>
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>Transaction</th>
                      <th>Type</th>
                      <th>Status</th>
                      <th>From</th>
                      <th>To</th>
                      <th className="num">Amount</th>
                      <th>Notes</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.items.map((t) => (
                      <tr key={t.transactionId}>
                        <td>{formatTimestamp(t.createdAt)}</td>
                        <td>
                          <Link to={`/admin/transactions/${t.transactionId}`}>{t.transactionId}</Link>
                        </td>
                        <td>{t.type}</td>
                        <td>
                          <StatusBadge value={t.status} />
                        </td>
                        <td>
                          <code>{t.sourceAccountId}</code>
                        </td>
                        <td>
                          <code>{t.destinationAccountId}</code>
                        </td>
                        <td className="num">
                          <Money minor={t.amountMinor} currency={t.currency} />
                        </td>
                        <td>
                          {t.failureCode && <span>{t.failureCode} </span>}
                          {t.reversedBy && <span>reversed by {t.reversedBy} </span>}
                          {t.reversalOfId && <span>reverses {t.reversalOfId}</span>}
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
