import { useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { DataState } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { Pagination } from '../../components/Pagination';
import { useApiQuery } from '../../hooks/useApiQuery';
import { formatTimestamp } from '../../lib/dates';
import { listAccountEntries } from './api';

/**
 * Entry rows carry no currency, so it comes from the link (?currency=INR) or from a System Funding ID
 * (SYS-FUND-INR). Without it, amounts are shown as raw minor units rather than guessed.
 */
function currencyOf(accountId: string, fromLink: string | null): string | undefined {
  if (fromLink && /^[A-Z]{3}$/.test(fromLink)) {
    return fromLink;
  }
  return /^SYS-FUND-([A-Z]{3})$/.exec(accountId)?.[1];
}

/** ADMIN: the ledger entries of one account (FR-ADM-03, FR-LED-04). */
export function AccountEntriesPage() {
  const { id = '' } = useParams();
  const [search] = useSearchParams();
  const currency = currencyOf(id, search.get('currency'));
  const [page, setPage] = useState(0);
  const entries = useApiQuery(`entries:${id}:${page}`, (c) => listAccountEntries(c, id, page));
  const amount = (minor: number) => (currency ? <Money minor={minor} currency={currency} /> : <span>{minor} (minor units)</span>);

  return (
    <>
      <h1>Ledger entries of {id}</h1>
      <section className="card">
        <DataState loading={entries.loading} error={entries.error} data={entries.data}>
          {(data) =>
            data.items.length === 0 ? (
              <p className="muted">This account has no ledger entries yet.</p>
            ) : (
              <>
                <table>
                  <thead>
                    <tr>
                      <th>Seq</th>
                      <th>Time</th>
                      <th>Transaction</th>
                      <th>Direction</th>
                      <th className="num">Amount</th>
                      <th className="num">Balance after</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.items.map((e) => (
                      <tr key={e.entryId}>
                        <td>{e.sequenceNo}</td>
                        <td>{formatTimestamp(e.createdAt)}</td>
                        <td>
                          <Link to={`/admin/transactions/${e.transactionId}`}>{e.transactionId}</Link>
                        </td>
                        <td>{e.direction}</td>
                        <td className="num">{amount(e.amountMinor)}</td>
                        <td className="num">{amount(e.balanceAfterMinor)}</td>
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
