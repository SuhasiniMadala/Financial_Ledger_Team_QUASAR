import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { formatMinor, MAX_AMOUNT_MINOR, parseRupeesToPaise } from './money';

describe('formatMinor (UI-01)', () => {
  // Same cases as the backend's MoneyFormatterTest, so client and server always show the same text.
  it.each([
    [0, '₹0.00'],
    [1, '₹0.01'],
    [99, '₹0.99'],
    [100, '₹1.00'],
    [100000, '₹1,000.00'],
    [500000, '₹5,000.00'],
    [925000, '₹9,250.00'],
    [10000000, '₹1,00,000.00'],
    [1234567891, '₹1,23,45,678.91'],
  ])('%i paise -> %s', (paise, expected) => {
    expect(formatMinor(paise, 'INR')).toBe(expected);
  });

  it('uses western grouping for USD', () => {
    expect(formatMinor(123456789, 'USD')).toBe('$1,234,567.89');
    expect(formatMinor(5, 'USD')).toBe('$0.05');
  });

  it('formats negative differences (reconciliation) and rejects non-integers', () => {
    expect(formatMinor(-50000, 'INR')).toBe('-₹500.00');
    expect(() => formatMinor(10.5, 'INR')).toThrow();
  });

  it('NFR-TEST-06: 10 + 20 paise is exactly 30 paise', () => {
    expect(formatMinor(10 + 20, 'INR')).toBe('₹0.30');
  });
});

describe('parseRupeesToPaise (UI-02)', () => {
  it.each([
    ['1', 100],
    ['1.5', 150],
    ['1.50', 150],
    ['0.01', 1],
    ['250', 25000],
    ['1000.00', 100000],
    ['1,00,000.00', 10000000],
    ['100,000', 10000000],
    ['  42.07 ', 4207],
    ['0.10', 10],
    // Classic floating-point traps: exact in paise.
    ['0.29', 29],
    ['1.15', 115],
    ['4.35', 435],
  ])('"%s" -> %i paise', (input, paise) => {
    expect(parseRupeesToPaise(input)).toEqual({ ok: true, paise });
  });

  it.each([
    ['', 'EMPTY'],
    ['   ', 'EMPTY'],
    ['1.505', 'TOO_MANY_DECIMALS'],
    ['100.5555', 'TOO_MANY_DECIMALS'],
    ['-1', 'FORMAT'],
    ['+1', 'FORMAT'],
    ['1e3', 'FORMAT'],
    ['abc', 'FORMAT'],
    ['1.', 'FORMAT'],
    ['.5', 'FORMAT'],
    ['1..2', 'FORMAT'],
    [',100', 'FORMAT'],
    ['0', 'ZERO'],
    ['0.00', 'ZERO'],
    ['100000.01', 'TOO_LARGE'],
    ['99999999999999999', 'TOO_LARGE'],
  ])('"%s" is rejected as %s', (input, reason) => {
    expect(parseRupeesToPaise(input)).toEqual({ ok: false, reason });
  });

  it('accepts exactly the per-transaction limit (BR-02)', () => {
    expect(parseRupeesToPaise('100000')).toEqual({ ok: true, paise: MAX_AMOUNT_MINOR });
  });
});

describe('TQ-6 guard: no floating-point money in the client', () => {
  const sourceFiles = (dir: string): string[] =>
    readdirSync(dir).flatMap((name) => {
      const path = join(dir, name);
      if (statSync(path).isDirectory()) return sourceFiles(path);
      return /\.(ts|tsx)$/.test(name) && !name.endsWith('.test.ts') && !name.endsWith('.test.tsx') ? [path] : [];
    });

  it('no source file uses parseFloat, toFixed or divides by 100', () => {
    const offenders = sourceFiles(join(__dirname, '..')).filter((file) =>
      /parseFloat\(|\.toFixed\(|\/\s*100\b/.test(readFileSync(file, 'utf8')),
    );
    expect(offenders).toEqual([]);
  });
});
