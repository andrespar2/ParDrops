/**
 * @file src/app/core/models/user.model.ts
 * @description Modelos relacionados con el usuario y la autenticación.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

/**
 * Roles disponibles para los usuarios.
 * - `CUSTOMER`: cliente que compra y consulta su historial.
 * - `SELLER`: vendedor con tienda propia y panel privado.
 * - `ADMIN`: administrador que gestiona la plataforma.
 */
export type UserRole = 'CUSTOMER' | 'SELLER' | 'ADMIN';

/**
 * Origen de autenticación de la cuenta.
 * - `LOCAL`: creada con correo y contraseña.
 * - `GOOGLE`: creada o vinculada con Google.
 */
export type AuthProvider = 'LOCAL' | 'GOOGLE';

/**
 * Usuario autenticado tal y como lo devuelve la API.
 *
 * @interface User
 */
export interface User {
  /** Identificador interno del usuario. */
  id: number;
  /** Correo electrónico (usuario de acceso). */
  email: string;
  /** Nombre completo. */
  fullName: string;
  /** Teléfono de contacto (opcional). */
  phone?: string | null;
  /** Rol del usuario en la plataforma. */
  role: UserRole;
  /** Estado de la cuenta. */
  status?: 'ACTIVE' | 'SUSPENDED';
  /** Origen de la autenticación de la cuenta. */
  provider?: AuthProvider;
  /** Foto de perfil del proveedor externo, si existe. */
  avatarUrl?: string | null;
}

/**
 * Respuesta de la API tras iniciar sesión o registrarse.
 *
 * @interface AuthResponse
 */
export interface AuthResponse {
  /** Token JWT que acredita la sesión. */
  token: string;
  /** Datos públicos del usuario. */
  user: User;
}

/**
 * Credenciales para iniciar sesión.
 *
 * @interface LoginRequest
 */
export interface LoginRequest {
  /** Correo registrado. */
  email: string;
  /** Contraseña. */
  password: string;
}

/**
 * Datos necesarios para crear una cuenta.
 *
 * @interface RegisterRequest
 */
export interface RegisterRequest {
  /** Correo electrónico (único). */
  email: string;
  /** Contraseña (mínimo 6 caracteres). */
  password: string;
  /** Nombre completo. */
  fullName: string;
  /** Teléfono de contacto (opcional). */
  phone?: string;
}
