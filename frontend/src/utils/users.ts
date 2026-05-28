import type { HospitalServiceResponse, RoleOptionResponse } from '../api/types';

export function roleLabelFor(roles: RoleOptionResponse[], name: string): string {
  return roles.find((r) => r.name === name)?.label ?? name.replace(/^ROLE_/, '').replace(/_/g, ' ');
}

export function serviceNamesFor(ids: number[], catalog: HospitalServiceResponse[]): string {
  if (ids.length === 0) return '—';
  const byId = new Map(catalog.map((s) => [s.id, s.name]));
  return ids.map((id) => byId.get(id) ?? `#${id}`).join(', ');
}

export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

/** Nom complet tel que construit côté serveur à la création. */
export function buildUserFullNamePreview(firstName: string, lastName: string, postName: string): string {
  const parts = [firstName.trim(), lastName.trim(), postName.trim()].filter(Boolean);
  return parts.length > 0 ? parts.join(' ') : '';
}

/** Aperçu d'identifiant de connexion (prénom + nom, sans point ; suffixe numérique possible si doublon). */
/** Prénom / nom dérivés du nom complet (aperçu mot de passe en modification). */
export function splitFullNameForPassword(fullName: string): { firstName: string; lastName: string } {
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return { firstName: '', lastName: '' };
  }
  if (parts.length === 1) {
    return { firstName: parts[0], lastName: parts[0] };
  }
  return { firstName: parts[0], lastName: parts[parts.length - 1] };
}

export function buildUserUsernamePreview(fullName: string): string {
  const parts = fullName.trim().toLowerCase().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '';
  const base = parts.length >= 2 ? `${parts[0]}${parts[parts.length - 1]}` : parts[0];
  return base.replace(/[^a-z0-9]/g, '');
}
