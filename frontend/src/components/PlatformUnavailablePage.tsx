import { Link } from 'react-router-dom';

export function PlatformUnavailablePage({ title, description }: { title: string; description: string }) {
  return (
    <div className="card" style={{ maxWidth: 640 }}>
      <h1 className="page-title">{title}</h1>
      <p style={{ color: 'var(--muted)', marginTop: 0 }}>{description}</p>
      <p style={{ marginBottom: 0 }}>
        <Link to="/">Retour au tableau de bord</Link>
      </p>
    </div>
  );
}
