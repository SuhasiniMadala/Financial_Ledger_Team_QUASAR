import { Link } from 'react-router-dom';

export function ForbiddenPage() {
  return (
    <div className="card">
      <h2>Not allowed</h2>
      <p>Your role cannot open this screen.</p>
      <Link to="/">Back to my home page</Link>
    </div>
  );
}
