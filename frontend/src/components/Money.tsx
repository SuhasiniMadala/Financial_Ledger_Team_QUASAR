import { formatMinor } from '../lib/money';

/** Every amount on every screen is rendered through formatMinor (UI-01). */
export function Money({ minor, currency }: { minor: number; currency: string }) {
  return <span className="money">{formatMinor(minor, currency)}</span>;
}
