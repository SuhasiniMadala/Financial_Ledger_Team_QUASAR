import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { ApiError } from '../api/errors';
import { ERROR_MESSAGES, messageFor } from './errorMessages';

const ERROR_CODE_JAVA = join(__dirname, '../../../src/main/java/com/quasar/ledger/common/api/ErrorCode.java');

describe('messageFor (UI-03)', () => {
  it('has a message for every error code the backend can return (read from ErrorCode.java)', () => {
    const java = readFileSync(ERROR_CODE_JAVA, 'utf8');
    const backendCodes = [...java.matchAll(/^\s+([A-Z_]+)\(HttpStatus\./gm)].map((m) => m[1]);
    expect(backendCodes.length).toBe(17);
    expect(Object.keys(ERROR_MESSAGES).sort()).toEqual([...backendCodes].sort());
  });

  it('adds the failed transaction ID for recorded 422 rejections', () => {
    const error = new ApiError({
      kind: 'http',
      status: 422,
      message: 'x',
      body: { code: 'INSUFFICIENT_FUNDS', message: 'x', transactionId: 'T-0A1B2C3D4E5F' },
    });
    expect(messageFor(error)).toBe(`${ERROR_MESSAGES.INSUFFICIENT_FUNDS} Recorded as failed transaction T-0A1B2C3D4E5F.`);
  });

  it('shows the field detail for validation errors', () => {
    const error = new ApiError({ kind: 'http', status: 400, message: 'amountMinor: must be at least 1', body: { code: 'VALIDATION_ERROR', message: 'amountMinor: must be at least 1' } });
    expect(messageFor(error)).toContain('(amountMinor: must be at least 1)');
  });

  it('tells the user a retry is safe after a network error or timeout', () => {
    expect(messageFor(new ApiError({ kind: 'network', message: 'x' }))).toMatch(/Retry: it is safe/);
    expect(messageFor(new ApiError({ kind: 'timeout', message: 'x' }))).toMatch(/Retry: it is safe/);
  });
});
