export type TableSortDir = 'asc' | 'desc';

/** Bascule tri colonne : même colonne → inverse l’ordre, nouvelle colonne → ordre par défaut. */
export function toggleTableSort<T extends string>(
  column: T,
  currentColumn: T,
  setSortBy: (column: T) => void,
  setSortDir: (update: (dir: TableSortDir) => TableSortDir) => void,
  defaultDirForNewColumn: TableSortDir = 'asc',
): void {
  if (currentColumn === column) {
    setSortDir((dir) => (dir === 'asc' ? 'desc' : 'asc'));
  } else {
    setSortBy(column);
    setSortDir(() => defaultDirForNewColumn);
  }
}

/** Ordre initial conseillé pour une nouvelle colonne (dates / ID souvent en descendant). */
export function defaultSortDirForColumn(column: string): TableSortDir {
  if (
    column === 'id' ||
    column === 'birthDate' ||
    column === 'admissionDateTime' ||
    column === 'consultationDateTime' ||
    column === 'recordedAt' ||
    column === 'createdAt' ||
    column === 'occurredAt' ||
    column === 'active'
  ) {
    return 'desc';
  }
  return 'asc';
}

export function compareStrings(
  a: string | null | undefined,
  b: string | null | undefined,
  dir: TableSortDir,
): number {
  const aVal = a ?? '';
  const bVal = b ?? '';
  const cmp = aVal.localeCompare(bVal, 'fr', { sensitivity: 'base' });
  return dir === 'asc' ? cmp : -cmp;
}

export function compareNumbers(a: number, b: number, dir: TableSortDir): number {
  const cmp = a - b;
  return dir === 'asc' ? cmp : -cmp;
}
