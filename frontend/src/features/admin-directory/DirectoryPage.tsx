import { useState } from 'react';
import { DataState } from '../../components/Feedback';
import { Pagination } from '../../components/Pagination';
import { useApiQuery } from '../../hooks/useApiQuery';
import { formatTimestamp } from '../../lib/dates';
import { listMerchants, listUsers, type UserSummary } from './api';

function PeopleTable({ people, merchants }: { people: UserSummary[]; merchants: boolean }) {
  if (people.length === 0) {
    return <p className="muted">None registered yet.</p>;
  }
  return (
    <table>
      <thead>
        <tr>
          <th>Account ID</th>
          <th>{merchants ? 'Business name' : 'Name'}</th>
          <th>Email</th>
          {merchants && <th>Merchant code</th>}
          <th>Registered</th>
        </tr>
      </thead>
      <tbody>
        {people.map((p) => (
          <tr key={p.accountId}>
            <td>
              <code>{p.accountId}</code>
            </td>
            <td>{p.name}</td>
            <td>{p.email}</td>
            {merchants && (
              <td>
                <code>{p.merchantCode}</code>
              </td>
            )}
            <td>{formatTimestamp(p.registeredAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/** ADMIN Users & Merchants (SRS 3.1, FR-ADM-01). */
export function DirectoryPage() {
  const [userPage, setUserPage] = useState(0);
  const [merchantPage, setMerchantPage] = useState(0);
  const users = useApiQuery(`users:${userPage}`, (c) => listUsers(c, userPage));
  const merchants = useApiQuery(`merchants:${merchantPage}`, (c) => listMerchants(c, merchantPage));
  return (
    <>
      <h1>Users and merchants</h1>
      <section className="card">
        <h2>Users</h2>
        <DataState loading={users.loading} error={users.error} data={users.data}>
          {(page) => (
            <>
              <PeopleTable people={page.items} merchants={false} />
              <Pagination page={page.page} totalPages={page.totalPages} totalItems={page.totalItems} onPage={setUserPage} />
            </>
          )}
        </DataState>
      </section>
      <section className="card">
        <h2>Merchants</h2>
        <DataState loading={merchants.loading} error={merchants.error} data={merchants.data}>
          {(page) => (
            <>
              <PeopleTable people={page.items} merchants />
              <Pagination page={page.page} totalPages={page.totalPages} totalItems={page.totalItems} onPage={setMerchantPage} />
            </>
          )}
        </DataState>
      </section>
    </>
  );
}
