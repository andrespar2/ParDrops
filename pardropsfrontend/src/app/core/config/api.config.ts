/**
 * @file src/app/core/config/api.config.ts
 * @description Configuración central de acceso a la API y a la persistencia local.
 *
 * Reúne en un único lugar la URL base del backend y las claves usadas en
 * localStorage, para poder cambiarlas sin tocar el resto del código.
 */

/** URL base de la API REST del backend Spring Boot. */
export const API_BASE_URL = 'http://localhost:8080/api';

/** Claves de almacenamiento local usadas por los servicios. */
export const STORAGE_KEYS = {
  /** Token JWT de la sesión activa. */
  token: 'pardrops_token',
  /** Datos públicos del usuario autenticado. */
  user: 'pardrops_user',
  /** Carrito de compras. */
  cart: 'pardrops_cart'
} as const;
