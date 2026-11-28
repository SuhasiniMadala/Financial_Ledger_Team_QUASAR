import { useCallback, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useApiClient } from '../../auth/AuthContext';
import { SuccessBanner, ReplayedNote } from '../../components/Feedback';
import { FinancialForm } from '../../components/FinancialForm';
import { Money } from '../../components/Money';
import { useFinancialSubmit } from '../../hooks/useFinancialSubmit';
import { PARSE_MESSAGES, parseRupeesToPaise } from '../../lib/money';
import { testFund } from './api';

/**
 * ADMIN test funding (SRS 4.2, FR-WAL-07/08): the only way money enters the system. Posts DEBIT System Funding /
 * CREDIT wallet in one transaction. Uses the shared idempotent submit (UI-04, UI-05).
 */
export function TestFundingPage() {
  const { walletId = '' } = useParams();
  const client = useApiClient();
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [validation, setValidation] = useState<string>();
  const send = useCallback(
    (body: { amountMinor: number; description?: string }, key: string) => testFund(client, walletId, body, key),
    [client, walletId],
  );
  const funding = useFinancialSubmit(send);

  const submit = () => {
    const parsed = parseRupeesToPaise(amount);
    if (!parsed.ok) {
      setValidation(PARSE_MESSAGES[parsed.reason]);
      return;
    }
    setValidation(undefined);
    void funding.submit({ amountMinor: parsed.paise, ...(description.trim() ? { description: description.trim() } : {}) });
  };

  if (funding.result) {
    const r = funding.result;
    return (
      <div className="card">
        <h1>Test funding recorded</h1>
        <SuccessBanner>
          <Money minor={r.amountMinor} currency={r.currency} /> credited to {r.destinationAccountId}. Transaction{' '}
          <Link to={`/admin/transactions/${r.transactionId}`}>{r.transactionId}</Link> ({r.status}).
        </SuccessBanner>
        <ReplayedNote replayed={funding.replayed} />
        <div className="actions">
          <button type="button" onClick={() => { funding.reset(); setAmount(''); setDescription(''); }}>
            Fund again
          </button>
          <Link to="/admin/wallets">Back to wallets</Link>
        </div>
      </div>
    );
  }

  return (
    <FinancialForm
      title={`Test funding for ${walletId}`}
      submitLabel="Add simulated funds"
      inFlight={funding.inFlight}
      canRetry={funding.canRetry}
      error={funding.error}
      replayed={funding.replayed}
      onSubmit={submit}
      onRetry={() => void funding.retry()}
      validationMessage={validation}
    >
      <p className="muted">Simulated money only. Only ACTIVE user wallets can be funded.</p>
      <label>
        Amount in rupees
        <input inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="5000.00" />
      </label>
      <label>
        Description (optional, up to 140 characters)
        <input value={description} maxLength={140} onChange={(e) => setDescription(e.target.value)} />
      </label>
    </FinancialForm>
  );
}
