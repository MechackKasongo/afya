import axios from 'axios';

type ApiErrorPayload = {
  message?: string;
  error?: string;
};

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiErrorPayload | undefined;
    if (typeof data?.message === 'string' && data.message.trim()) return data.message;
    if (typeof data?.error === 'string' && data.error.trim()) return data.error;
    const status = error.response?.status;
    if (status === 401) {
      return 'Accès refusé (401). Reconnectez-vous ou redémarrez le BFF (port 8080) après une mise à jour du code.';
    }
    if (status === 403) {
      return 'Action réservée au rôle administrateur (403).';
    }
    if (status === 409) {
      return data?.message?.trim() || 'Conflit : cette valeur est déjà utilisée.';
    }
    if (status === 502 || status === 503) {
      return 'Service indisponible (gateway ou BFF). Vérifiez que afya-bff et catalog-service sont démarrés.';
    }
  }
  return fallback;
}
