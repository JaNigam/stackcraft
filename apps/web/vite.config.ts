/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import mdx from '@mdx-js/rollup';
import remarkGfm from 'remark-gfm';
import path from 'node:path';

const repoRoot = path.resolve(__dirname, '../..');

// Content lives outside the web root (content as code, shared with the api). Vite compiles theory.mdx at build
// time and bundles chapter YAML + verified code output, so the site also works as a static, offline-capable build.
export default defineConfig({
  plugins: [
    // remark-gfm: GitHub-style tables, task lists and strikethrough in theory.mdx.
    { enforce: 'pre', ...mdx({ providerImportSource: '@mdx-js/react', remarkPlugins: [remarkGfm] }) },
    react({ include: /\.(mdx|jsx|tsx|ts|js)$/ }),
  ],
  resolve: {
    alias: { '@content': path.join(repoRoot, 'content'), '@': path.resolve(__dirname, 'src') },
    // MDX files outside the web root must still use this app's single copy of React.
    dedupe: ['react', 'react-dom', '@mdx-js/react'],
  },
  server: {
    port: 5173,
    strictPort: true,
    fs: { allow: [repoRoot] },
    proxy: { '/api': process.env.VITE_API_PROXY ?? 'http://localhost:8080' },
  },
  preview: {
    port: 4173,
    proxy: { '/api': process.env.VITE_API_PROXY ?? 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
});
