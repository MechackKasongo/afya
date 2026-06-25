import { Link, useLocation, useSearchParams } from 'react-router-dom';

const TABS = [
  { id: 'occupation', label: 'Occupation' },
  { id: 'stats', label: 'Labo & soins' },
  { id: 'audit', label: "Journal d'audit" },
] as const;

export type ReportingTabId = (typeof TABS)[number]['id'];

export function ReportingTabs() {
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const active = resolveReportingTab(searchParams);

  function href(tabId: ReportingTabId) {
    const next = new URLSearchParams(searchParams);
    if (tabId === 'occupation') {
      next.delete('tab');
    } else {
      next.set('tab', tabId);
    }
    const qs = next.toString();
    return `${location.pathname}${qs ? `?${qs}` : ''}`;
  }

  return (
    <nav className="page-tabs" aria-label="Sections du reporting">
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

export function resolveReportingTab(searchParams: URLSearchParams): ReportingTabId {
  const tab = searchParams.get('tab');
  if (tab === 'audit') return 'audit';
  if (tab === 'stats') return 'stats';
  return 'occupation';
}
