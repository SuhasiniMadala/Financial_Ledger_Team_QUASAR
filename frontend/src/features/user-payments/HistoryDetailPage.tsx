import { Link, useParams } from 'react-router-dom';
import { DataState, StatusBadge } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { useApiQuery } from '../../hooks/useApiQuery';
import { formatTimestamp } from '../../lib/dates';
import { getHistoryItem } from './api';

/**
 * USER transaction detail (SRS 4.6.2 #6, FR-LED-12): type, status, amount, time, description, counterparty, failure
 * code, the user's OWN entries and the reversal link - never the other party's entries or balance.
 */
export function HistoryDetailPage() {
  const { id = '' } = useParams();
  const detail = useApiQuery(`history-detail:${id}`, (c) => getHistoryItem(c, id));
  return (
    <DataState loading={detail.loading} error={detail.error} data={detail.data}>
      {({ transaction: t, ownEntries }) => (
        <>
          <h1>Transaction {t.transactionId}</h1>
          <section className="card">
            <dl className="details">
              <dt>Type</dt>
              <dd>{t.type}</dd>
              <dt>Status</dt>
              <dd>
                <StatusBadge value={t.status} />
              </dd>
              <dt>Direction</dt>
              <dd>{t.direction === 'OUTGOING' ? 'Money out' : 'Money in'}</dd>
              <dt>Amount</dt>
              <dd>
                <Money minor={t.amountMinor} currency={t.currency} />
              </dd>
              <dt>Time</dt>
              <dd>{formatTimestamp(t.createdAt)}</dd>
              <dt>My wallet</dt>
              <dd>
                <code>{t.walletId}</code>
              </dd>
              {t.counterpartyName && (
                <>
                  <dt>Counterparty</dt>
                  <dd>{t.counterpartyName}</dd>
                </>
              )}
              {t.description && (
                <>
                  <dt>Description</dt>
                  <dd>{t.description}</dd>
                </>
              )}
              {t.failureCode && (
                <>
                  <dt>Failure code</dt>
                  <dd>{t.failureCode}</dd>
                </>
              )}
              {t.reversalOfId && (
                <>
                  <dt>Reverses</dt>
                  <dd>
                    <Link to={`/user/history/${t.reversalOfId}`}>{t.reversalOfId}</Link>
                  </dd>
                </>
              )}
              {t.reversedBy && (
                <>
                  <dt>Reversed by</dt>
                  <dd>
                    <Link to={`/user/history/${t.reversedBy}`}>{t.reversedBy}</Link>
                  </dd>
                </>
              )}
            </dl>
          </section>
          <section className="card">
            <h2>My ledger entries</h2>
            {ownEntries.length === 0 ? (
              <p className="muted">No entries: this attempt FAILED and moved no money.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Wallet</th>
                    <th>Direction</th>
                    <th className="num">Amount</th>
                    <th className="num">My balance after</th>
                  </tr>
                </thead>
                <tbody>
                  {ownEntries.map((e) => (
                    <tr key={e.entryId}>
                      <td>
                        <code>{e.accountId}</code>
                      </td>
                      <td>{e.direction}</td>
                      <td className="num">
                        <Money minor={e.amountMinor} currency={t.currency} />
                      </td>
                      <td className="num">
                        <Money minor={e.balanceAfterMinor} currency={t.currency} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
          <Link to="/user/history">Back to history</Link>
        </>
      )}
    </DataState>
  );
}
