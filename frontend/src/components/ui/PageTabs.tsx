import { NavLink } from 'react-router-dom';

export type PageTabItem = {
  to: string;
  label: string;
  end?: boolean;
};

type PageTabsProps = {
  tabs: PageTabItem[];
  ariaLabel?: string;
};

export function PageTabs({ tabs, ariaLabel = 'Sections' }: PageTabsProps) {
  return (
    <nav className="page-tabs" aria-label={ariaLabel}>
      {tabs.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          end={tab.end}
          className={({ isActive }) => `page-tabs__tab${isActive ? ' page-tabs__tab--active' : ''}`}
        >
          {tab.label}
        </NavLink>
      ))}
    </nav>
  );
}
