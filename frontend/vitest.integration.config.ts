import { defineConfig } from 'vitest/config';

// Integration tests talk to a REAL running backend (no mocks). Start it first; see tests/integration/README.md.
export default defineConfig({
  test: {
    environment: 'node',
    include: ['tests/integration/**/*.int.test.ts'],
    testTimeout: 60_000,
    hookTimeout: 60_000,
    fileParallelism: false,
  },
});
