import { Link } from 'react-router-dom';
import type { TransactionReceipt } from '../../api/types';
import { ReplayedNote, SuccessBanner } from '../../components/Feedback';
import { Money } from '../../components/Money';

/** Success is only ever shown from the backend's 201 receipt; the client never computes a new balance itself. */
export function Receipt({
  title,
  receipt,
  replayed,
  onAgain,
  againLabel,
}: {
  title: string;
  receipt: TransactionReceipt;
  replayed: boolean;
  onAgain: () => void;
  againLabel: string;
}) {
  return (
    <div className="card">
      <h1>{title}</h1>
      <SuccessBanner>
        <Money minor={receipt.amountMinor} currency={receipt.currency} /> sent from {receipt.sourceAccountId} to{' '}
        {receipt.destinationAccountId}. Transaction{' '}
        <Link to={`/user/history/${receipt.transactionId}`}>{receipt.transactionId}</Link> is {receipt.status}.
      </SuccessBanner>
      <ReplayedNote replayed={replayed} />
      <div className="actions">
        <button type="button" onClick={onAgain}>
          {againLabel}
        </button>
        <Link to="/user/dashboard">See my balances</Link>
      </div>
    </div>
  );
}

/** For 422 rejections the backend records a FAILED transaction; link to it so the user can see it in history. */
export function FailedLink({ transactionId }: { transactionId?: string }) {
  return transactionId ? (
    <p>
      <Link to={`/user/history/${transactionId}`}>Open failed transaction {transactionId}</Link>
    </p>
  ) : null;
}
