import type { ReactNode } from 'react';

type DrawerProps = {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  footer?: ReactNode;
  width?: 'md' | 'lg';
};

export function Drawer({ open, onClose, title, children, footer, width = 'lg' }: DrawerProps) {
  if (!open) {
    return null;
  }

  return (
    <>
      <div className="drawer-overlay" role="presentation" onClick={onClose} />
      <aside
        className={`drawer-panel drawer-panel--${width}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="drawer-title"
        onMouseDown={(e) => e.stopPropagation()}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="drawer-panel__head">
          <h2 id="drawer-title" className="drawer-panel__title">
            {title}
          </h2>
          <button type="button" className="btn btn-ghost btn-sm" onClick={onClose}>
            Fermer
          </button>
        </div>
        <div className="drawer-panel__body">{children}</div>
        {footer ? <div className="drawer-panel__foot">{footer}</div> : null}
      </aside>
    </>
  );
}
