const MAX_LENGTH = 40;

/** Code technique dérivé du nom (aligné sur catalog-service DepartmentCodeGenerator). */
export function departmentCodeFromName(name: string): string {
  const trimmed = name.trim();
  if (!trimmed) {
    return 'DEPT';
  }
  const normalized = trimmed
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/[^a-zA-Z0-9]/g, '')
    .toUpperCase();
  if (!normalized) {
    return 'DEPT';
  }
  return normalized.length <= MAX_LENGTH ? normalized : normalized.slice(0, MAX_LENGTH);
}

export function departmentCodeWithSuffix(baseCode: string, suffix: number): string {
  if (suffix <= 1) {
    return baseCode;
  }
  const suffixPart = String(suffix);
  const maxBase = MAX_LENGTH - suffixPart.length;
  const trimmed = baseCode.length <= maxBase ? baseCode : baseCode.slice(0, maxBase);
  return trimmed + suffixPart;
}
