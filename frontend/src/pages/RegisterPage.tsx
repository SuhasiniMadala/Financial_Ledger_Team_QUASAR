import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { toApiError, type ApiError } from '../api/errors';
import { useApiClient } from '../auth/AuthContext';
import { ErrorBanner, SuccessBanner } from '../components/Feedback';

interface RegistrationResponse {
  accountId: string;
  role: string;
  email: string;
  displayName: string;
  merchantCode?: string;
  walletId?: string;
}

type Kind = 'USER' | 'MERCHANT';

/** FR-AUTH-01/02: visitors register as USER or MERCHANT. ADMIN accounts never come from here (FR-AUTH-05). */
export function RegisterPage() {
  const client = useApiClient();
  const [kind, setKind] = useState<Kind>('USER');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<ApiError>();
  const [localError, setLocalError] = useState<string>();
  const [done, setDone] = useState<RegistrationResponse>();

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(undefined);
    // FR-AUTH-04, checked again by the backend.
    if (password.length < 8 || password.length > 72) {
      setLocalError('The password must be 8 to 72 characters.');
      return;
    }
    if (!name.trim()) {
      setLocalError(kind === 'USER' ? 'Enter your name.' : 'Enter the business name.');
      return;
    }
    setLocalError(undefined);
    setBusy(true);
    try {
      const body =
        kind === 'USER'
          ? { displayName: name.trim(), email: email.trim(), password }
          : { businessName: name.trim(), email: email.trim(), password };
      const path = kind === 'USER' ? '/auth/register' : '/auth/register/merchant';
      setDone(await client.post<RegistrationResponse>(path, body));
    } catch (caught) {
      setError(toApiError(caught));
    } finally {
      setBusy(false);
    }
  };

  if (done) {
    return (
      <div className="auth-page">
        <h1>Financial Ledger</h1>
        <div className="card">
          <SuccessBanner>Account created for {done.displayName}.</SuccessBanner>
          {done.merchantCode && (
            <p>
              Merchant code <strong>{done.merchantCode}</strong>, INR wallet <strong>{done.walletId}</strong> (balance ₹0.00).
            </p>
          )}
          <Link to="/login">Go to login</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <h1>Financial Ledger</h1>
      <form className="card form" onSubmit={submit}>
        <h2>Register</h2>
        <fieldset className="choice">
          <legend>Account type</legend>
          <label>
            <input type="radio" name="kind" checked={kind === 'USER'} onChange={() => setKind('USER')} /> User
          </label>
          <label>
            <input type="radio" name="kind" checked={kind === 'MERCHANT'} onChange={() => setKind('MERCHANT')} /> Merchant
          </label>
        </fieldset>
        <label>
          {kind === 'USER' ? 'Display name' : 'Business name'}
          <input value={name} maxLength={100} onChange={(e) => setName(e.target.value)} />
        </label>
        <label>
          Email
          <input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label>
          Password (8 to 72 characters)
          <input type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>
        {localError && (
          <p className="field-error" role="alert">
            {localError}
          </p>
        )}
        <ErrorBanner error={error} />
        <div className="actions">
          <button type="submit" disabled={busy}>
            {busy ? 'Registering...' : 'Register'}
          </button>
        </div>
        <p className="muted">
          Already registered? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  );
}
