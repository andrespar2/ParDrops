/**
 * Escribe la configuración de ejecución del frontend antes de compilar.
 *
 * Uso:
 *   PARDROPS_API_URL=https://pardrops-api.onrender.com/api npm run config:runtime
 *
 * Si la variable no está definida se deja el fichero versionado, que apunta al
 * backend local. Así el mismo repositorio sirve para desarrollo y producción
 * sin guardar direcciones de producción en el código.
 */
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const target = fileURLToPath(new URL('../public/config.js', import.meta.url));
const apiBaseUrl = (process.env.PARDROPS_API_URL ?? '').trim().replace(/\/+$/, '');

if (!apiBaseUrl) {
  console.log('[config] PARDROPS_API_URL no definida: se mantiene public/config.js');
  process.exit(0);
}

if (apiBaseUrl.includes("'")) {
  console.error('[config] PARDROPS_API_URL contiene comillas simples; revisa el valor.');
  process.exit(1);
}

writeFileSync(
  target,
  `window.__PARDROPS_CONFIG__ = {\n  apiBaseUrl: '${apiBaseUrl}'\n};\n`,
  'utf8',
);

console.log(`[config] public/config.js -> ${apiBaseUrl}`);
