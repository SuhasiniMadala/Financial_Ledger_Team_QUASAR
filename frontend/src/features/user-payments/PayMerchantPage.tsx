import { useCallback, useState } from 'react';
import { MERCHANT_CODE_PATTERN } from '../../api/types';
import { useApiClient } from '../../auth/AuthContext';
import { DataState } from '../../components/Feedback';
import { FinancialForm } from '../../components/FinancialForm';
import { useApiQuery } from '../../hooks/useApiQuery';
import { useFinancialSubmit } from '../../hooks/useFinancialSubmit';
import { PARSE_MESSAGES, parseRupeesToPaise } from '../../lib/money';
import { listWallets } from '../user-wallets/api';
import { payMerchant, type PaymentBody } from './api';
import { FailedLink, Receipt } from './Receipt';
import { SourceWalletSelect } from './SourceWalletSelect';

/** USER Pay Merchant (SRS 3.1, FR-PAY-01..03): same idempotency, validation and concurrency rules as transfers. */
export function PayMerchantPage() {
  const client = useApiClient();
  const wallets = useApiQuery('wallets', listWallets);
  const [source, setSource] = useState('');
  const [merchantCode, setMerchantCode] = useState('');
  const [amount, setAmount] = useState('');
  const [reference, setReference] = useState('');
  const [validation, setValidation] = useState<string>();
  const send = useCallback((body: PaymentBody, key: string) => payMerchant(client, body, key), [client]);
  const payment = useFinancialSubmit(send);

  const submit = () => {
    const code = merchantCode.trim().toUpperCase();
    if (!source) {
      setValidation('Choose the wallet to pay from.');
      return;
    }
    if (!MERCHANT_CODE_PATTERN.test(code)) {
      setValidation('A merchant code looks like MER- followed by 6 letters or digits, e.g. MER-7Q2K9D.');
      return;
    }
    const parsed = parseRupeesToPaise(amount);
    if (!parsed.ok) {
      setValidation(PARSE_MESSAGES[parsed.reason]);
      return;
    }
    setValidation(undefined);
    void payment.submit({
      sourceWalletId: source,
      merchantCode: code,
      amountMinor: parsed.paise,
      ...(reference.trim() ? { reference: reference.trim() } : {}),
    });
  };

  if (payment.result) {
    return (
      <Receipt
        title="Payment completed"
        receipt={payment.result}
        replayed={payment.replayed}
        againLabel="Make another payment"
        onAgain={() => {
          payment.reset();
          setAmount('');
          setReference('');
          wallets.reload();
        }}
      />
    );
  }

  return (
    <DataState loading={wallets.loading} error={wallets.error} data={wallets.data}>
      {(list) => (
        <FinancialForm
          title="Pay a merchant"
          submitLabel="Pay"
          inFlight={payment.inFlight}
          canRetry={payment.canRetry}
          error={payment.error}
          replayed={payment.replayed}
          onSubmit={submit}
          onRetry={() => void payment.retry()}
          validationMessage={validation}
        >
          <SourceWalletSelect wallets={list} value={source} onChange={setSource} />
          <label>
            Merchant code
            <input value={merchantCode} onChange={(e) => setMerchantCode(e.target.value)} placeholder="MER-XXXXXX" />
          </label>
          <label>
            Amount in rupees
            <input inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="250.00" />
          </label>
          <label>
            Reference (optional, up to 64 characters)
            <input value={reference} maxLength={64} onChange={(e) => setReference(e.target.value)} placeholder="Order 1187" />
          </label>
          <FailedLink transactionId={payment.error?.transactionId} />
        </FinancialForm>
      )}
    </DataState>
  );
}
