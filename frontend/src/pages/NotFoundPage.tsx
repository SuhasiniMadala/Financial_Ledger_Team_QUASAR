import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="card">
      <h2>Page not found</h2>
      <Link to="/">Back to my home page</Link>
    </div>
  );
}
