import type { ReactNode } from 'react';
import { DataState, StatusBadge } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { useApiQuery } from '../../hooks/useApiQuery';
import { formatTimestamp } from '../../lib/dates';
import { getStatistics } from './api';

function Stat({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="stat">
      <div className="label">{label}</div>
      <div className="value">{value}</div>
    </div>
  );
}

/** ADMIN Statistics (SRS 4.8.2 #3, FR-ADM-07). */
export function StatisticsPage() {
  const stats = useApiQuery('admin-stats', getStatistics);
  return (
    <>
      <h1>Platform statistics</h1>
      <DataState loading={stats.loading} error={stats.error} data={stats.data}>
        {(s) => (
          <>
            <div className="stats">
              <Stat label="Users" value={s.users} />
              <Stat label="Merchants" value={s.merchants} />
              {Object.entries(s.walletsByStatus).map(([status, count]) => (
                <Stat key={`w-${status}`} label={`Wallets ${status}`} value={count} />
              ))}
              {Object.entries(s.transactionsByStatus).map(([status, count]) => (
                <Stat key={`t-${status}`} label={`Transactions ${status}`} value={count} />
              ))}
            </div>
            <section className="card">
              <h2>Completed totals by type</h2>
              {s.completedTotals.length === 0 ? (
                <p className="muted">No completed transactions yet.</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>Currency</th>
                      <th className="num">Count</th>
                      <th className="num">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {s.completedTotals.map((t) => (
                      <tr key={`${t.type}-${t.currency}`}>
                        <td>{t.type}</td>
                        <td>{t.currency}</td>
                        <td className="num">{t.count}</td>
                        <td className="num">
                          <Money minor={t.totalMinor} currency={t.currency} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
            <section className="card">
              <h2>Latest reconciliation</h2>
              {s.latestReconciliation ? (
                <p>
                  <StatusBadge value={s.latestReconciliation.result} /> at {formatTimestamp(s.latestReconciliation.runAt)},{' '}
                  {s.latestReconciliation.discrepancyCount} discrepancies (run <code>{s.latestReconciliation.runId}</code>)
                </p>
              ) : (
                <p className="muted">Reconciliation has not been run yet.</p>
              )}
            </section>
          </>
        )}
      </DataState>
    </>
  );
}
