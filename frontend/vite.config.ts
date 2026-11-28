import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// The backend has no CORS configuration, so the browser must call it on the same origin.
// The dev server and the preview server forward every /api request to the Spring Boot backend (ADR-011).
const backend = process.env.LEDGER_API_URL ?? 'http://localhost:8080';
const proxy = { '/api': { target: backend, changeOrigin: false } };

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true, proxy },
  preview: { port: 4173, strictPort: true, proxy },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    restoreMocks: true,
  },
});
