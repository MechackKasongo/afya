import { useEffect } from 'react';

export const TOAST_DISMISS_MS = 5000;

type ToastProps = {
  message: string | null;
  onDismiss: () => void;
};

/** Message de confirmation compact, fixé en haut à droite, masqué après 5 s. */
export function Toast({ message, onDismiss }: ToastProps) {
  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(onDismiss, TOAST_DISMISS_MS);
    return () => window.clearTimeout(timer);
  }, [message, onDismiss]);

  if (!message) return null;

  return (
    <div className="app-toast" role="status" aria-live="polite">
      {message}
    </div>
  );
}
