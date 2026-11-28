import { NavLink, Outlet } from 'react-router-dom';
import { useAuth, useSession } from '../auth/AuthContext';
import { navFor } from '../features/registry';

/** SRS 3.1: top bar (product name, account name, Logout) and role-specific navigation. */
export function Layout() {
  const session = useSession();
  const { logout } = useAuth();
  return (
    <div className="app">
      <header className="top-bar">
        <span className="product">Financial Ledger</span>
        <span className="account" data-testid="account-name">
          {session.displayName} <span className="role-tag">{session.role}</span>
        </span>
        <button type="button" className="secondary" onClick={() => logout()}>
          Logout
        </button>
      </header>
      <nav className="role-nav" aria-label="Main navigation">
        {navFor(session.role).map((item) => (
          <NavLink key={item.path} to={item.path}>
            {item.label}
          </NavLink>
        ))}
      </nav>
      <main className="content">
        <Outlet />
      </main>
      <footer className="footer">Simulated money only. No bank or payment network is connected.</footer>
    </div>
  );
}
