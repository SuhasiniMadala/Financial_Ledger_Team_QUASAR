import { useCallback, useState } from 'react';
import { WALLET_ID_PATTERN } from '../../api/types';
import { useApiClient } from '../../auth/AuthContext';
import { DataState } from '../../components/Feedback';
import { FinancialForm } from '../../components/FinancialForm';
import { useApiQuery } from '../../hooks/useApiQuery';
import { useFinancialSubmit } from '../../hooks/useFinancialSubmit';
import { PARSE_MESSAGES, parseRupeesToPaise } from '../../lib/money';
import { listWallets } from '../user-wallets/api';
import { sendTransfer, type TransferBody } from './api';
import { FailedLink, Receipt } from './Receipt';
import { SourceWalletSelect } from './SourceWalletSelect';

/**
 * USER Transfer (SRS 3.1, FR-TRF-01..14). Amounts are parsed to integer paise (UI-02); the request carries a new
 * Idempotency-Key and the button is disabled while it is in flight (UI-04, UI-05). All business rules
 * (balance, currency, destination type, status) are enforced by the backend; the client only pre-checks formats.
 */
export function TransferPage() {
  const client = useApiClient();
  const wallets = useApiQuery('wallets', listWallets);
  const [source, setSource] = useState('');
  const [destination, setDestination] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [validation, setValidation] = useState<string>();
  const send = useCallback((body: TransferBody, key: string) => sendTransfer(client, body, key), [client]);
  const transfer = useFinancialSubmit(send);

  const submit = () => {
    const dest = destination.trim().toUpperCase();
    if (!source) {
      setValidation('Choose the wallet to send from.');
      return;
    }
    if (!WALLET_ID_PATTERN.test(dest)) {
      setValidation('The destination wallet ID looks like W- followed by 10 letters or digits.');
      return;
    }
    const parsed = parseRupeesToPaise(amount);
    if (!parsed.ok) {
      setValidation(PARSE_MESSAGES[parsed.reason]);
      return;
    }
    setValidation(undefined);
    void transfer.submit({
      sourceWalletId: source,
      destinationWalletId: dest,
      amountMinor: parsed.paise,
      ...(description.trim() ? { description: description.trim() } : {}),
    });
  };

  if (transfer.result) {
    return (
      <Receipt
        title="Transfer completed"
        receipt={transfer.result}
        replayed={transfer.replayed}
        againLabel="Make another transfer"
        onAgain={() => {
          transfer.reset();
          setAmount('');
          setDescription('');
          wallets.reload();
        }}
      />
    );
  }

  return (
    <DataState loading={wallets.loading} error={wallets.error} data={wallets.data}>
      {(list) => (
        <FinancialForm
          title="Transfer to another user"
          submitLabel="Send money"
          inFlight={transfer.inFlight}
          canRetry={transfer.canRetry}
          error={transfer.error}
          replayed={transfer.replayed}
          onSubmit={submit}
          onRetry={() => void transfer.retry()}
          validationMessage={validation}
        >
          <SourceWalletSelect wallets={list} value={source} onChange={setSource} />
          <label>
            To wallet ID
            <input value={destination} onChange={(e) => setDestination(e.target.value)} placeholder="W-XXXXXXXXXX" />
          </label>
          <label>
            Amount in rupees
            <input inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="1000.00" />
          </label>
          <label>
            Description (optional, up to 140 characters)
            <input value={description} maxLength={140} onChange={(e) => setDescription(e.target.value)} />
          </label>
          <FailedLink transactionId={transfer.error?.transactionId} />
        </FinancialForm>
      )}
    </DataState>
  );
}
