import { useState, type FormEvent } from 'react';
import { DateRangeFilter } from '../../components/DateRangeFilter';
import { DataState } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { useApiQuery } from '../../hooks/useApiQuery';
import { getMerchantStatistics, type DateRange } from './api';

/**
 * MERCHANT Statistics (SRS 3.1, FR-PAY-07): count, total and average of completed payments for a date range
 * (default: the last 30 days). The average is computed by the backend, rounded half-even to the nearest paisa.
 */
export function MerchantStatisticsPage() {
  const [draft, setDraft] = useState({ from: '', to: '' });
  const [applied, setApplied] = useState<DateRange>({});
  const stats = useApiQuery(`merchant-stats:${JSON.stringify(applied)}`, (c) => getMerchantStatistics(c, applied));

  const apply = (event: FormEvent) => {
    event.preventDefault();
    setApplied({ from: draft.from || undefined, to: draft.to || undefined });
  };

  return (
    <>
      <h1>Payment statistics</h1>
      <form className="card filters" onSubmit={apply}>
        <DateRangeFilter from={draft.from} to={draft.to} onChange={setDraft} />
        <button type="submit">Apply</button>
      </form>
      <DataState loading={stats.loading} error={stats.error} data={stats.data}>
        {(s) => (
          <>
            <p className="muted">
              From {s.from} to {s.to} (UTC, inclusive)
            </p>
            <div className="stats">
              <div className="stat">
                <div className="label">Completed payments</div>
                <div className="value" data-testid="payment-count">
                  {s.paymentCount}
                </div>
              </div>
              <div className="stat">
                <div className="label">Total</div>
                <div className="value">
                  <Money minor={s.totalMinor} currency={s.currency} />
                </div>
              </div>
              <div className="stat">
                <div className="label">Average</div>
                <div className="value">
                  <Money minor={s.averageMinor} currency={s.currency} />
                </div>
              </div>
            </div>
          </>
        )}
      </DataState>
    </>
  );
}
