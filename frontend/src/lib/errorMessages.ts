import { ApiError } from '../api/errors';

/**
 * UI-03: every error code of SRS Table 3-2 has a readable sentence. The backend's own message is shown
 * underneath for VALIDATION_ERROR, because it names the field that was wrong.
 */
export const ERROR_MESSAGES: Record<string, string> = {
  VALIDATION_ERROR: 'Some of the information is not valid. Please check it and try again.',
  IDEMPOTENCY_KEY_REQUIRED: 'The request was sent without its safety key. Please try again.',
  UNAUTHENTICATED: 'The email or password is not correct, or your session has expired.',
  FORBIDDEN: 'Your role is not allowed to do this.',
  NOT_FOUND: 'Not found, or it does not belong to you.',
  DUPLICATE_RESOURCE: 'This already exists (an account with this email, or a wallet in this currency).',
  IDEMPOTENCY_KEY_REUSED: 'This request conflicts with an earlier one. Start a new payment and submit it again.',
  ALREADY_REVERSED: 'This transaction has already been reversed.',
  INVALID_STATE_TRANSITION: 'This wallet status change is not allowed (a deactivated wallet cannot change again).',
  INSUFFICIENT_FUNDS: 'There is not enough money in the wallet for this amount.',
  WALLET_NOT_ACTIVE: 'One of the wallets is frozen or deactivated, so it cannot take part.',
  CURRENCY_MISMATCH: 'Both wallets must use the same currency. There is no currency conversion.',
  SAME_WALLET: 'You cannot send money to the same wallet.',
  INVALID_DESTINATION: 'The destination is not valid. Users send to user wallets and pay merchants by merchant code.',
  NOT_REVERSIBLE: 'Failed transactions and reversals cannot be reversed.',
  INTERNAL_ERROR: 'Something went wrong on the server. Nothing was changed.',
  SERVICE_BUSY: 'The ledger is busy. Nothing was changed. Press Retry to send the same request again.',
};

export function messageFor(error: ApiError): string {
  if (error.kind === 'timeout') {
    return 'The server did not answer in time. The payment may or may not have gone through. Press Retry: it is safe and will not charge twice.';
  }
  if (error.kind === 'network') {
    return 'Could not reach the server. Check that the backend is running, then press Retry: it is safe and will not charge twice.';
  }
  const base = (error.code && ERROR_MESSAGES[error.code]) ?? 'Unexpected error.';
  const parts = [base];
  if (error.code === 'VALIDATION_ERROR' && error.message) {
    parts.push(`(${error.message})`);
  }
  if (error.transactionId) {
    parts.push(`Recorded as failed transaction ${error.transactionId}.`);
  }
  return parts.join(' ');
}
