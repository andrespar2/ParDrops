/**
 * @file src/app/core/config/api.config.ts
 * @description Configuración central de acceso a la API y a la persistencia local.
 *
 * Reúne en un único lugar la URL base del backend y las claves usadas en
 * localStorage, para poder cambiarlas sin tocar el resto del código.
 *
 * La URL de la API se resuelve en tiempo de ejecución desde
 * `public/config.js` (`window.__PARDROPS_CONFIG__.apiBaseUrl`). Así el mismo
 * bundle compilado sirve para desarrollo (localhost) y para producción
 * (Cloudflare + Render) sin recompilar ni dejar direcciones fijas en el código.
 */

/** Valor por defecto para el desarrollo local. */
const DEFAULT_API_BASE_URL = 'http://localhost:8080/api';

declare global {
  interface Window {
    __PARDROPS_CONFIG__?: {
      /** URL base de la API, incluyendo el sufijo /api. */
      apiBaseUrl?: string;
    };
  }
}

/**
 * Resuelve la URL de la API: primero la configuración publicada en
 * `config.js`, después el valor de desarrollo.
 *
 * @returns la URL base sin barra final.
 */
function resolveApiBaseUrl(): string {
  if (typeof window !== 'undefined') {
    const configured = window.__PARDROPS_CONFIG__?.apiBaseUrl?.trim();
    if (configured) {
      return configured.replace(/\/+$/, '');
    }
  }
  return DEFAULT_API_BASE_URL;
}

/** URL base de la API REST del backend Spring Boot. */
export const API_BASE_URL = resolveApiBaseUrl();

/** Origen del backend (sin la ruta /api), usado para las redirecciones OAuth2. */
export const API_ORIGIN = API_BASE_URL.replace(/\/api$/, '');

/** Inicio del flujo de autorización de Google en el backend. */
export const GOOGLE_AUTH_URL = `${API_ORIGIN}/oauth2/authorization/google`;

/** Clave de sesión donde se guarda la ruta a la que volver tras el acceso. */
export const OAUTH_REDIRECT_KEY = 'pardrops_oauth_redirect';

/** Claves de almacenamiento local usadas por los servicios. */
export const STORAGE_KEYS = {
  /** Token JWT de la sesión activa. */
  token: 'pardrops_token',
  /** Datos públicos del usuario autenticado. */
  user: 'pardrops_user',
  /** Carrito de compras. */
  cart: 'pardrops_cart'
} as const;
