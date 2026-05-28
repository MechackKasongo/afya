import type { ReactNode } from 'react';

export type { TableSortDir } from '../utils/tableSort';
import type { TableSortDir } from '../utils/tableSort';

type DataTableColumnHeaderProps = {
  label: string;
  sortable?: boolean;
  sortActive?: boolean;
  sortDir?: TableSortDir;
  onSort?: () => void;
  filterable?: boolean;
  filterOpen?: boolean;
  filterActive?: boolean;
  onToggleFilter?: () => void;
};

function FilterIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <path d="M4 6h16M7 12h10M10 18h4" strokeLinecap="round" />
    </svg>
  );
}

export function DataTableColumnHeader({
  label,
  sortable = false,
  sortActive = false,
  sortDir = 'desc',
  onSort,
  filterable = false,
  filterOpen = false,
  filterActive = false,
  onToggleFilter,
}: DataTableColumnHeaderProps) {
  return (
    <div className="table-column-header">
      {sortable ? (
        <button type="button" className="table-column-header__label" onClick={onSort}>
          {label}
          <span className={`table-column-header__sort${sortActive ? ' is-active' : ''}`}>
            {sortDir === 'asc' ? '↑' : '↓'}
          </span>
        </button>
      ) : (
        <span className="table-column-header__label table-column-header__label--static">{label}</span>
      )}
      {filterable ? (
        <button
          type="button"
          className={`table-column-header__filter${filterOpen ? ' is-open' : ''}${filterActive ? ' is-active' : ''}`}
          onClick={onToggleFilter}
          title={filterActive ? 'Filtre actif' : 'Filtrer'}
          aria-label={`Filtrer : ${label}`}
          aria-expanded={filterOpen}
        >
          <FilterIcon />
        </button>
      ) : null}
    </div>
  );
}

type DataTableFilterCellProps = {
  open: boolean;
  value: string;
  onChange: (value: string) => void;
  onApply: () => void;
  onClear: () => void;
  placeholder: string;
  inputMode?: 'numeric' | 'search' | 'text';
};

export function DataTableFilterCell({
  open,
  value,
  onChange,
  onApply,
  onClear,
  placeholder,
  inputMode = 'text',
}: DataTableFilterCellProps) {
  if (!open) return null;
  return (
    <div className="table-column-filter">
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault();
            onApply();
          }
        }}
        placeholder={placeholder}
        inputMode={inputMode}
        aria-label={placeholder}
      />
      <button type="button" className="btn btn-primary btn-sm" onClick={onApply}>
        OK
      </button>
      <button type="button" className="btn btn-ghost btn-sm" onClick={onClear}>
        ×
      </button>
    </div>
  );
}

type DataTableFilterSelectOption = {
  value: string;
  label: string;
};

type DataTableFilterSelectCellProps = {
  open: boolean;
  value: string;
  onChange: (value: string) => void;
  onApply: () => void;
  onClear: () => void;
  options: DataTableFilterSelectOption[];
  ariaLabel: string;
  allLabel?: string;
};

export function DataTableFilterSelectCell({
  open,
  value,
  onChange,
  onApply,
  onClear,
  options,
  ariaLabel,
  allLabel = 'Tous les types',
}: DataTableFilterSelectCellProps) {
  if (!open) return null;
  return (
    <div className="table-column-filter">
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault();
            onApply();
          }
        }}
        aria-label={ariaLabel}
      >
        <option value="">{allLabel}</option>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
      <button type="button" className="btn btn-primary btn-sm" onClick={onApply}>
        OK
      </button>
      <button type="button" className="btn btn-ghost btn-sm" onClick={onClear}>
        ×
      </button>
    </div>
  );
}

export function DataTableFilterHint({ children }: { children: ReactNode }) {
  return <p className="table-column-filter-hint">{children}</p>;
}
