import { useCallback, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useApiClient } from '../../auth/AuthContext';
import { DataState, ReplayedNote, StatusBadge, SuccessBanner } from '../../components/Feedback';
import { FinancialForm } from '../../components/FinancialForm';
import { Money } from '../../components/Money';
import { useApiQuery } from '../../hooks/useApiQuery';
import { useFinancialSubmit } from '../../hooks/useFinancialSubmit';
import { formatTimestamp } from '../../lib/dates';
import { entryTotals, getTransaction, reverseTransaction, type AdminTransaction } from './api';

function ReversalForm({ transaction, onDone }: { transaction: AdminTransaction; onDone: () => void }) {
  const client = useApiClient();
  const [reason, setReason] = useState('');
  const [validation, setValidation] = useState<string>();
  const send = useCallback(
    (body: { reason: string }, key: string) => reverseTransaction(client, transaction.transactionId, body, key),
    [client, transaction.transactionId],
  );
  const reversal = useFinancialSubmit(send);

  if (reversal.result) {
    return (
      <div className="card">
        <SuccessBanner>
          Reversed by <Link to={`/admin/transactions/${reversal.result.transactionId}`}>{reversal.result.transactionId}</Link>{' '}
          (mirror-image entries). The original is unchanged.
        </SuccessBanner>
        <ReplayedNote replayed={reversal.replayed} />
        <button type="button" className="secondary" onClick={onDone}>
          Refresh this transaction
        </button>
      </div>
    );
  }

  const submit = () => {
    const text = reason.trim();
    if (text.length < 10 || text.length > 500) {
      setValidation('The reason must be 10 to 500 characters.');
      return;
    }
    setValidation(undefined);
    void reversal.submit({ reason: text });
  };

  return (
    <FinancialForm
      title="Reverse this transaction"
      submitLabel="Reverse"
      inFlight={reversal.inFlight}
      canRetry={reversal.canRetry}
      error={reversal.error}
      replayed={reversal.replayed}
      onSubmit={submit}
      onRetry={() => void reversal.retry()}
      validationMessage={validation}
    >
      <p className="muted">
        Creates a new REVERSAL transaction with the same accounts and amount in opposite directions (FR-LED-07). A
        transaction can be reversed only once.
      </p>
      <label>
        Reason (10 to 500 characters)
        <textarea rows={2} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />
      </label>
    </FinancialForm>
  );
}

/** ADMIN transaction detail: all ledger entries, the debit = credit check, reversal links and the reversal action. */
export function TransactionDetailPage() {
  const { id = '' } = useParams();
  const detail = useApiQuery(`admin-tx-detail:${id}`, (c) => getTransaction(c, id));

  return (
    <DataState loading={detail.loading} error={detail.error} data={detail.data}>
      {({ transaction: t, entries }) => {
        const totals = entryTotals(entries);
        const reversible = t.status === 'COMPLETED' && t.type !== 'REVERSAL' && !t.reversedBy;
        return (
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
                <dt>Amount</dt>
                <dd>
                  <Money minor={t.amountMinor} currency={t.currency} /> ({t.amountMinor} minor units)
                </dd>
                <dt>Time</dt>
                <dd>{formatTimestamp(t.createdAt)}</dd>
                <dt>Initiated by</dt>
                <dd>
                  <code>{t.initiatedBy}</code>
                </dd>
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
                      <Link to={`/admin/transactions/${t.reversalOfId}`}>{t.reversalOfId}</Link>
                    </dd>
                  </>
                )}
                {t.reversedBy && (
                  <>
                    <dt>Reversed by</dt>
                    <dd>
                      <Link to={`/admin/transactions/${t.reversedBy}`}>{t.reversedBy}</Link>
                    </dd>
                  </>
                )}
              </dl>
            </section>
            <section className="card">
              <h2>Ledger entries</h2>
              {entries.length === 0 ? (
                <p className="muted">No entries. FAILED transactions never post entries (FR-LED-03).</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Seq</th>
                      <th>Account</th>
                      <th>Direction</th>
                      <th className="num">Amount</th>
                      <th className="num">Balance after</th>
                    </tr>
                  </thead>
                  <tbody>
                    {entries.map((e) => (
                      <tr key={e.entryId}>
                        <td>{e.sequenceNo}</td>
                        <td>
                          <Link to={`/admin/accounts/${e.accountId}/entries?currency=${t.currency}`}>{e.accountId}</Link>
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
              {entries.length > 0 && (
                <p data-testid="balance-check">
                  Debits <Money minor={totals.debits} currency={t.currency} /> = credits{' '}
                  <Money minor={totals.credits} currency={t.currency} />:{' '}
                  <strong>{totals.debits === totals.credits ? 'balanced' : 'NOT BALANCED'}</strong>
                </p>
              )}
            </section>
            {reversible && <ReversalForm transaction={t} onDone={detail.reload} />}
          </>
        );
      }}
    </DataState>
  );
}
