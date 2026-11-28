// Money in the client (UI-01, UI-02, TQ-6, ADR-004).
// Amounts are integer minor units (paise for INR) everywhere. This file never uses floating-point arithmetic:
// formatting and parsing work on digit strings, exactly like the backend's MoneyFormatter.

/** Backend default per-transaction limit (BR-02, ADR-008): Rs 1,00,000.00 = 10,000,000 paise. */
export const MAX_AMOUNT_MINOR = 10_000_000;

function groupIndian(digits: string): string {
  if (digits.length <= 3) {
    return digits;
  }
  const last3 = digits.slice(-3);
  const rest = digits.slice(0, -3);
  const groups: string[] = [];
  const first = rest.length % 2;
  if (first > 0) {
    groups.push(rest.slice(0, first));
  }
  for (let i = first; i < rest.length; i += 2) {
    groups.push(rest.slice(i, i + 2));
  }
  return `${groups.join(',')},${last3}`;
}

function groupWestern(digits: string): string {
  const groups: string[] = [];
  const lead = digits.length % 3;
  if (lead > 0) {
    groups.push(digits.slice(0, lead));
  }
  for (let i = lead; i < digits.length; i += 3) {
    groups.push(digits.slice(i, i + 3));
  }
  return groups.join(',');
}

function symbolFor(currency: string): string {
  if (currency === 'INR') return '₹';
  if (currency === 'USD') return '$';
  return `${currency} `;
}

/**
 * UI-01: Rs symbol, Indian digit grouping and two decimals, e.g. 10000000 -> "₹1,00,000.00".
 * Gives the same output as the backend's MoneyFormatter (checked in money.test.ts).
 */
export function formatMinor(amountMinor: number, currency: string): string {
  if (!Number.isSafeInteger(amountMinor)) {
    throw new Error(`Money must be an integer number of minor units, got ${amountMinor}`);
  }
  const negative = amountMinor < 0;
  const digits = String(Math.abs(amountMinor)).padStart(3, '0');
  const major = digits.slice(0, -2).replace(/^0+(?=\d)/, '');
  const minor = digits.slice(-2);
  const grouped = currency === 'INR' ? groupIndian(major) : groupWestern(major);
  return `${negative ? '-' : ''}${symbolFor(currency)}${grouped}.${minor}`;
}

export type ParseFailure = 'EMPTY' | 'FORMAT' | 'TOO_MANY_DECIMALS' | 'ZERO' | 'TOO_LARGE';
export type ParseResult = { ok: true; paise: number } | { ok: false; reason: ParseFailure };

export const PARSE_MESSAGES: Record<ParseFailure, string> = {
  EMPTY: 'Enter an amount.',
  FORMAT: 'Enter an amount in rupees, for example 250 or 250.50.',
  TOO_MANY_DECIMALS: 'Use at most two decimal places (paise).',
  ZERO: 'The amount must be at least ₹0.01.',
  TOO_LARGE: 'The amount is above the per-transaction limit of ₹1,00,000.00.',
};

/**
 * UI-02: converts a typed rupee amount to integer paise by exact string parsing.
 * Accepts "250", "250.5", "250.50" and Indian or western grouping commas ("1,00,000.00").
 * Rejects more than two decimals, signs, exponents, zero and anything above the limit.
 */
export function parseRupeesToPaise(input: string, maxPaise: number = MAX_AMOUNT_MINOR): ParseResult {
  const text = input.trim();
  if (text === '') {
    return { ok: false, reason: 'EMPTY' };
  }
  if (!/^[\d,]+(\.\d*)?$/.test(text) || text.startsWith(',') || text.includes(',,') || /,\./.test(text)) {
    return { ok: false, reason: 'FORMAT' };
  }
  const [rupeesPart = '', paisePart] = text.replace(/,/g, '').split('.');
  if (paisePart !== undefined && paisePart.length > 2) {
    return { ok: false, reason: 'TOO_MANY_DECIMALS' };
  }
  if (paisePart !== undefined && paisePart.length === 0) {
    return { ok: false, reason: 'FORMAT' };
  }
  const rupees = rupeesPart.replace(/^0+(?=\d)/, '');
  if (rupees.length > 13) {
    // Far above any limit; also keeps every value a safe integer.
    return { ok: false, reason: 'TOO_LARGE' };
  }
  // Concatenate digit strings: "250" + "50" -> "25050" paise. No multiplication of a decimal number.
  const paise = Number(`${rupees}${(paisePart ?? '').padEnd(2, '0')}`);
  if (paise === 0) {
    return { ok: false, reason: 'ZERO' };
  }
  if (paise > maxPaise) {
    return { ok: false, reason: 'TOO_LARGE' };
  }
  return { ok: true, paise };
}
