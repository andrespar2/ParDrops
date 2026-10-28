/**
 * @file src/app/core/config/api.config.ts
 * @description Configuración central de acceso a la API y a la persistencia local.
 *
 * Reúne en un único lugar la URL base del backend y las claves usadas en
 * localStorage, para poder cambiarlas sin tocar el resto del código.
 */

/** URL base de la API REST del backend Spring Boot. */
export const API_BASE_URL = 'http://localhost:8080/api';

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
