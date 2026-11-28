import { useState, type FormEvent } from 'react';
import { DateRangeFilter } from '../../components/DateRangeFilter';
import { DataState } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { Pagination } from '../../components/Pagination';
import { useApiQuery } from '../../hooks/useApiQuery';
import { formatTimestamp } from '../../lib/dates';
import { listMerchantTransactions, type DateRange } from './api';

/** MERCHANT Payments (SRS 3.1, FR-PAY-05/06): payments and refunds (reversals), newest first, by date range. */
export function PaymentsPage() {
  const [draft, setDraft] = useState({ from: '', to: '' });
  const [applied, setApplied] = useState<DateRange>({});
  const [page, setPage] = useState(0);
  const txs = useApiQuery(`merchant-tx:${JSON.stringify(applied)}:${page}`, (c) => listMerchantTransactions(c, applied, page));

  const apply = (event: FormEvent) => {
    event.preventDefault();
    setApplied({ from: draft.from || undefined, to: draft.to || undefined });
    setPage(0);
  };

  return (
    <>
      <h1>Payments received</h1>
      <form className="card filters" onSubmit={apply}>
        <DateRangeFilter from={draft.from} to={draft.to} onChange={setDraft} />
        <button type="submit">Apply</button>
      </form>
      <section className="card">
        <DataState loading={txs.loading} error={txs.error} data={txs.data}>
          {(data) =>
            data.items.length === 0 ? (
              <p className="muted">No payments in this period.</p>
            ) : (
              <>
                <table>
                  <thead>
                    <tr>
                      <th>Time</th>
                      <th>Transaction</th>
                      <th>Type</th>
                      <th>Payer</th>
                      <th>Reference</th>
                      <th className="num">Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.items.map((t) => (
                      <tr key={t.transactionId}>
                        <td>{formatTimestamp(t.createdAt)}</td>
                        <td>
                          <code>{t.transactionId}</code>
                        </td>
                        <td>
                          {t.type === 'REVERSAL' ? `Refund of ${t.reversalOfId ?? ''}` : 'Payment'}
                        </td>
                        <td>{t.payerName ?? ''}</td>
                        <td>{t.reference ?? ''}</td>
                        <td className="num">
                          {t.direction === 'OUTGOING' ? '- ' : '+ '}
                          <Money minor={t.amountMinor} currency={t.currency} />
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
