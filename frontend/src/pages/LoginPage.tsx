import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { toApiError, type ApiError } from '../api/errors';
import { useAuth } from '../auth/AuthContext';
import { HOME_BY_ROLE } from '../auth/session';
import { ErrorBanner } from '../components/Feedback';

/** FR-AUTH-06: all three roles log in here; a failed login shows one generic message. */
export function LoginPage() {
  const { session, login, notice } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<ApiError>();

  if (session) {
    return <Navigate to={HOME_BY_ROLE[session.role]} replace />;
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(undefined);
    try {
      const next = await login(email.trim(), password);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && from.startsWith(`/${next.role.toLowerCase()}`) ? from : HOME_BY_ROLE[next.role], { replace: true });
    } catch (caught) {
      setError(toApiError(caught));
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      <h1>Financial Ledger</h1>
      <form className="card form" onSubmit={submit}>
        <h2>Log in</h2>
        {notice && <div className="banner info">{notice}</div>}
        <label>
          Email
          <input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <ErrorBanner error={error} />
        <div className="actions">
          <button type="submit" disabled={busy}>
            {busy ? 'Logging in...' : 'Log in'}
          </button>
        </div>
        <p className="muted">
          No account? <Link to="/register">Register as a user or merchant</Link>
        </p>
      </form>
    </div>
  );
}
