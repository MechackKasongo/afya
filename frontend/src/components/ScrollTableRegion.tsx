import type { ReactNode, UIEvent } from 'react';

/**
 * Zone tableau avec défilement vertical à la place des boutons Précédent / Suivant.
 */
export function ScrollTableRegion({
  children,
  onScroll,
}: {
  children: ReactNode;
  onScroll?: (event: UIEvent<HTMLDivElement>) => void;
}) {
  return (
    <div className="table-scroll-region" onScroll={onScroll}>
      {children}
    </div>
  );
}

type TableResultFooterProps = {
  totalElements: number;
  displayedCount: number;
  /** Libellé déjà au pluriel affichable, ex. « patient(s) », « admission(s) » */
  itemLabelPlural: string;
};

export function TableResultFooter({ totalElements, displayedCount, itemLabelPlural }: TableResultFooterProps) {
  const truncated = displayedCount < totalElements;
  return (
    <p className="table-result-footer">
      {truncated ? (
        <>
          Affichés <strong>{displayedCount}</strong> sur <strong>{totalElements}</strong> {itemLabelPlural}.
          <span className="table-result-footer__hint">Affinez les filtres ou la recherche pour réduire la liste.</span>
        </>
      ) : (
        <>
          <strong>{totalElements}</strong> {itemLabelPlural}
        </>
      )}
    </p>
  );
}
