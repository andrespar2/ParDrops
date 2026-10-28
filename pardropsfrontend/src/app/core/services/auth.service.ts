/**
 * @file src/app/core/services/auth.service.ts
 * @description Servicio de autenticación y sesión del usuario.
 *
 * Gestiona el registro, el inicio y cierre de sesión, y expone el estado del
 * usuario mediante signals para que toda la interfaz reaccione a él. El token
 * JWT y los datos del usuario se persisten en localStorage para mantener la
 * sesión entre recargas.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { API_BASE_URL, GOOGLE_AUTH_URL, STORAGE_KEYS } from '../config/api.config';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  User
} from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  /** Cliente HTTP para llamar a la API. */
  private readonly http = inject(HttpClient);

  /** URL base de los endpoints de autenticación. */
  private readonly authUrl = `${API_BASE_URL}/auth`;

  /** Signal privado con el token JWT actual (o nulo si no hay sesión). */
  private readonly _token = signal<string | null>(this.readToken());

  /** Signal privado con el usuario autenticado (o nulo). */
  private readonly _user = signal<User | null>(this.readUser());

  /** Token de la sesión actual (solo lectura). */
  public readonly token = this._token.asReadonly();

  /** Usuario autenticado (solo lectura). */
  public readonly user = this._user.asReadonly();

  /** Indica si hay una sesión iniciada. */
  public readonly isAuthenticated = computed(() => this._user() !== null);

  /** Indica si el usuario actual es administrador. */
  public readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  /** Indica si el usuario actual es vendedor. */
  public readonly isSeller = computed(() => this._user()?.role === 'SELLER');

  /**
   * Inicia sesión con correo y contraseña.
   *
   * @param credentials credenciales del usuario.
   * @returns observable que emite el usuario autenticado.
   */
  public login(credentials: LoginRequest): Observable<User> {
    return this.http
      .post<AuthResponse>(`${this.authUrl}/login`, credentials)
      .pipe(
        tap(response => this.storeSession(response)),
        map(response => response.user)
      );
  }

  /**
   * Registra una cuenta nueva y deja la sesión iniciada.
   *
   * @param data datos del registro.
   * @returns observable que emite el usuario creado.
   */
  public register(data: RegisterRequest): Observable<User> {
    return this.http
      .post<AuthResponse>(`${this.authUrl}/register`, data)
      .pipe(
        tap(response => this.storeSession(response)),
        map(response => response.user)
      );
  }

  /**
   * Inicia el acceso con Google.
   *
   * Es una navegación de nivel superior al backend, que a su vez redirige a
   * Google. El JWT no viaja en esta URL: al volver, el backend entrega un
   * código temporal que se canjea en `/auth/callback`.
   *
   * @returns {void}
   */
  public loginWithGoogle(): void {
    if (typeof window === 'undefined') return;
    window.location.assign(GOOGLE_AUTH_URL);
  }

  /**
   * Cierra la sesión borrando el token y los datos del usuario.
   *
   * @returns {void}
   */
  public logout(): void {
    this._token.set(null);
    this._user.set(null);

    if (typeof window !== 'undefined') {
      localStorage.removeItem(STORAGE_KEYS.token);
      localStorage.removeItem(STORAGE_KEYS.user);
    }
  }

  /**
   * Guarda en memoria y en localStorage la sesión recién creada.
   *
   * @param response respuesta de la API con token y usuario.
   * @returns {void}
   * @private
   */
  private storeSession(response: AuthResponse): void {
    this._token.set(response.token);
    this._user.set(response.user);

    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.token, response.token);
      localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(response.user));
    }
  }

  /**
   * Lee el token guardado en localStorage (si existe).
   *
   * @returns el token o nulo.
   * @private
   */
  private readToken(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(STORAGE_KEYS.token);
  }

  /**
   * Lee el usuario guardado en localStorage (si existe).
   *
   * @returns el usuario o nulo.
   * @private
   */
  private readUser(): User | null {
    if (typeof window === 'undefined') return null;
    const raw = localStorage.getItem(STORAGE_KEYS.user);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  }
}
