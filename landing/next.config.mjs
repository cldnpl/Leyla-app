import { fileURLToPath } from 'node:url';
import { dirname } from 'node:path';

/** @type {import('next').NextConfig} */
const nextConfig = {
  // Senza questo Next elegge la home come workspace root (per via di
  // ~/package-lock.json) e finisce per tracciare tutto iCloud Drive.
  outputFileTracingRoot: dirname(fileURLToPath(import.meta.url)),
};

export default nextConfig;
