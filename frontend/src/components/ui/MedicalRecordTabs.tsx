import { Link, useSearchParams } from 'react-router-dom';

const TABS = [
  { id: 'resume', label: 'Résumé' },
  { id: 'stays', label: 'Séjours' },
  { id: 'global', label: 'Dossier global' },
] as const;

export type MedicalRecordTabId = (typeof TABS)[number]['id'];

export function MedicalRecordTabs() {
  const [searchParams] = useSearchParams();
  const active = (searchParams.get('tab') as MedicalRecordTabId | null) ?? 'resume';

  function href(tabId: string) {
    const next = new URLSearchParams(searchParams);
    next.set('tab', tabId);
    return `?${next.toString()}`;
  }

  return (
    <nav className="page-tabs" aria-label="Sections du dossier médical">
      {TABS.map((tab) => (
        <Link
          key={tab.id}
          to={href(tab.id)}
          className={`page-tabs__tab${active === tab.id ? ' page-tabs__tab--active' : ''}`}
        >
          {tab.label}
        </Link>
      ))}
    </nav>
  );
}
