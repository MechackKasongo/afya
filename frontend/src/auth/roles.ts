import type { MeResponse } from '../api/types';

export type AppRole = 'ROLE_ADMIN' | 'ROLE_MEDECIN' | 'ROLE_INFIRMIER' | 'ROLE_RECEPTION';

const APP_ROLES: AppRole[] = ['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER', 'ROLE_RECEPTION'];

function toAppRole(raw: string): AppRole | null {
  const normalized = raw.startsWith('ROLE_') ? raw : `ROLE_${raw}`;
  return APP_ROLES.includes(normalized as AppRole) ? (normalized as AppRole) : null;
}

/** Normalise les rôles renvoyés par identity (`ADMIN`) vers le format UI (`ROLE_ADMIN`). */
export function normalizeMe(user: MeResponse): MeResponse {
  const roles = Array.from(
    new Set((user.roles ?? []).map((r) => toAppRole(r)).filter((r): r is AppRole => r != null)),
  );
  return {
    ...user,
    roles,
    hospitalServiceIds: user.hospitalServiceIds ?? [],
    hospitalServiceNames: user.hospitalServiceNames ?? [],
  };
}

export function hasRole(user: MeResponse | null, role: AppRole): boolean {
  if (!user?.roles?.length) return false;
  const normalized = toAppRole(role);
  return normalized != null && user.roles.some((r) => toAppRole(r) === normalized);
}

export function hasAnyRole(user: MeResponse | null, roles: AppRole[]): boolean {
  return roles.some((role) => hasRole(user, role));
}

/** Rôles opérationnels (accueil, soins) — exclus du portail admin. */
export const CLINICAL_STAFF_ROLES: AppRole[] = ['ROLE_RECEPTION', 'ROLE_MEDECIN', 'ROLE_INFIRMIER'];

/** Compte administrateur plateforme : interface dédiée, sans parcours clinique. */
export function isAdminPortalUser(user: MeResponse | null): boolean {
  return hasRole(user, 'ROLE_ADMIN');
}

export function isClinicalStaffUser(user: MeResponse | null): boolean {
  return hasAnyRole(user, CLINICAL_STAFF_ROLES);
}
