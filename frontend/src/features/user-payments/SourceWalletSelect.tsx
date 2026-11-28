import type { Wallet } from '../../api/types';
import { formatMinor } from '../../lib/money';

/** Only ACTIVE wallets can send money (BR-07); the balance is shown so the user can see what is available. */
export function SourceWalletSelect({
  wallets,
  value,
  onChange,
}: {
  wallets: Wallet[];
  value: string;
  onChange: (walletId: string) => void;
}) {
  const active = wallets.filter((w) => w.status === 'ACTIVE');
  return (
    <label>
      From wallet
      <select value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">Choose a wallet</option>
        {active.map((w) => (
          <option key={w.walletId} value={w.walletId}>
            {w.walletId} - {w.currency} - {formatMinor(w.balanceMinor, w.currency)}
          </option>
        ))}
      </select>
    </label>
  );
}
