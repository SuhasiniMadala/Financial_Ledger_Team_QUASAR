import { DataState, StatusBadge } from '../../components/Feedback';
import { Money } from '../../components/Money';
import { useApiQuery } from '../../hooks/useApiQuery';
import { getMerchantAccount } from './api';

/** MERCHANT Account (SRS 3.1, FR-PAY-04). Merchants can only view; they cannot send money (BR-04). */
export function AccountPage() {
  const account = useApiQuery('merchant-account', getMerchantAccount);
  return (
    <DataState loading={account.loading} error={account.error} data={account.data}>
      {(a) => (
        <>
          <h1>{a.businessName}</h1>
          <section className="card">
            <dl className="details">
              <dt>Merchant code</dt>
              <dd>
                <code>{a.merchantCode}</code> (customers pay you with this code)
              </dd>
              <dt>Wallet</dt>
              <dd>
                <code>{a.walletId}</code> ({a.currency})
              </dd>
              <dt>Balance</dt>
              <dd>
                <Money minor={a.balanceMinor} currency={a.currency} />
              </dd>
              <dt>Status</dt>
              <dd>
                <StatusBadge value={a.status} />
              </dd>
            </dl>
          </section>
        </>
      )}
    </DataState>
  );
}
