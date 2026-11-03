import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Em desenvolvimento (npm run dev), repassa /api para o auth-service local.
    // Em produção quem faz esse papel é o nginx (ver nginx.conf).
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
});
