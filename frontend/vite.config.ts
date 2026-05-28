import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * DEV : proxy /api → API Gateway (8090) par défaut (phase E).
 * Sans gateway : VITE_API_PROXY_TARGET=http://127.0.0.1:8080 npm run dev
 */
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiTarget = env.VITE_API_PROXY_TARGET ?? 'http://127.0.0.1:8090';

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
