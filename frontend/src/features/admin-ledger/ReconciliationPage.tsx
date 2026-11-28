import { useState } from 'react';
import { toApiError, type ApiError } from '../../api/errors';
import { useApiClient } from '../../auth/AuthContext';
import { ErrorBanner, StatusBadge } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { formatTimestamp } from '../../lib/dates';
import { runReconciliation, type ReconciliationReport } from './api';

/**
 * ADMIN Reconciliation (SRS 4.7, FR-REC-01..08): runs C1 (debits = credits per currency), C2 (every COMPLETED
 * transaction balances, >= 2 entries) and C3 (stored balance = ledger balance) on one snapshot. Read-only: it reports
 * discrepancies and never repairs them.
 */
export function ReconciliationPage() {
  const client = useApiClient();
  const [running, setRunning] = useState(false);
  const [report, setReport] = useState<ReconciliationReport>();
  const [error, setError] = useState<ApiError>();

  const run = async () => {
    setRunning(true);
    setError(undefined);
    try {
      setReport(await runReconciliation(client));
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setRunning(false);
    }
  };

  return (
    <>
      <h1>Reconciliation</h1>
      <section className="card">
        <p className="muted">Checks the whole ledger against the stored balances. Nothing is changed.</p>
        <button type="button" onClick={() => void run()} disabled={running}>
          {running ? 'Running...' : 'Run reconciliation'}
        </button>
        <ErrorBanner error={error} />
      </section>
      {report && (
        <>
          <section className="card">
            <h2>
              Result <StatusBadge value={report.result} />
            </h2>
            <dl className="details">
              <dt>Run ID</dt>
              <dd>
                <code>{report.runId}</code>
              </dd>
              <dt>Run at</dt>
              <dd>{formatTimestamp(report.runAt)}</dd>
              <dt>Duration</dt>
              <dd>{report.durationMs} ms</dd>
              <dt>Checked</dt>
              <dd>
                {report.checked.accounts} accounts, {report.checked.transactions} transactions, {report.checked.entries} entries
              </dd>
              <dt>Discrepancies</dt>
              <dd>{report.discrepancies.length}</dd>
            </dl>
          </section>
          <section className="card">
            <h2>Totals per currency (C1)</h2>
            <table>
              <thead>
                <tr>
                  <th>Currency</th>
                  <th className="num">Total debits</th>
                  <th className="num">Total credits</th>
                  <th>Equal</th>
                </tr>
              </thead>
              <tbody>
                {report.currencies.map((c) => (
                  <tr key={c.currency}>
                    <td>{c.currency}</td>
                    <td className="num">
                      <Money minor={c.totalDebitsMinor} currency={c.currency} />
                    </td>
                    <td className="num">
                      <Money minor={c.totalCreditsMinor} currency={c.currency} />
                    </td>
                    <td>{c.totalDebitsMinor === c.totalCreditsMinor ? 'yes' : 'NO'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
          {report.discrepancies.length > 0 && (
            <section className="card">
              <h2>Discrepancies</h2>
              <table>
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Subject</th>
                    <th className="num">Expected (minor)</th>
                    <th className="num">Actual (minor)</th>
                    <th className="num">Difference (minor)</th>
                    <th>Detail</th>
                  </tr>
                </thead>
                <tbody>
                  {report.discrepancies.map((d, i) => (
                    <tr key={`${d.type}-${d.subjectId}-${i}`}>
                      <td>{d.type}</td>
                      <td>
                        <code>{d.subjectId}</code>
                      </td>
                      <td className="num">{d.expectedMinor}</td>
                      <td className="num">{d.actualMinor}</td>
                      <td className="num">{d.differenceMinor}</td>
                      <td>{d.detail}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}
        </>
      )}
    </>
  );
}
