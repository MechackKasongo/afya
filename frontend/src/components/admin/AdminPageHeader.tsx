import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { PageHeader } from '../ui/PageHeader';

type AdminPageHeaderProps = {
  title: string;
  subtitle?: ReactNode;
  children?: ReactNode;
};

/** En-tête des écrans réservés à l’administrateur plateforme. */
export function AdminPageHeader({ title, subtitle, children }: AdminPageHeaderProps) {
  return (
    <PageHeader
      title={title}
      subtitle={
        <>
          <Link to="/">Tableau de bord</Link>
          {subtitle ? <> — {subtitle}</> : null}
        </>
      }
    >
      {children}
    </PageHeader>
  );
}
