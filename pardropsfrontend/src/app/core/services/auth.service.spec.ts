/**
 * @file src/app/core/services/auth.service.spec.ts
 * @description Pruebas del servicio de autenticación y del acceso con Google.
 */

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { GOOGLE_AUTH_URL, OAUTH_REDIRECT_KEY, STORAGE_KEYS } from '../config/api.config';
import { User } from '../models/user.model';

const AUTH_URL = 'http://localhost:8080/api/auth';

const USER: User = {
  id: 7,
  email: 'cliente@pardrops.com',
  fullName: 'Cliente Prueba',
  phone: null,
  role: 'CUSTOMER',
  status: 'ACTIVE',
  provider: 'GOOGLE',
  avatarUrl: 'https://foto.example/cliente'
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('inicia sesión y persiste el token y el usuario', () => {
    let result: User | undefined;
    service.login({ email: USER.email, password: 'secreta' })
      .subscribe(user => (result = user));

    const request = httpMock.expectOne(`${AUTH_URL}/login`);
    expect(request.request.method).toBe('POST');
    request.flush({ token: 'jwt-login', user: USER });

    expect(result).toEqual(USER);
    expect(localStorage.getItem(STORAGE_KEYS.token)).toBe('jwt-login');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.isAdmin()).toBe(false);
    expect(service.isSeller()).toBe(false);
  });

  it('registra la cuenta y deja la sesión iniciada', () => {
    service.register({
      fullName: USER.fullName,
      email: USER.email,
      password: 'secreta'
    }).subscribe();

    httpMock.expectOne(`${AUTH_URL}/register`)
      .flush({ token: 'jwt-registro', user: USER });

    expect(service.token()).toBe('jwt-registro');
    expect(service.user()).toEqual(USER);
  });

  it('canjea el código de Google y guarda la sesión', () => {
    let result: User | undefined;
    service.exchangeGoogleCode('codigo-temporal')
      .subscribe(user => (result = user));

    const request = httpMock.expectOne(`${AUTH_URL}/google/exchange`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ code: 'codigo-temporal' });
    request.flush({ token: 'jwt-google', user: USER });

    expect(result).toEqual(USER);
    expect(localStorage.getItem(STORAGE_KEYS.token)).toBe('jwt-google');
    expect(service.user()?.avatarUrl).toBe(USER.avatarUrl);
  });

  it('cierra la sesión y limpia el almacenamiento', () => {
    localStorage.setItem(STORAGE_KEYS.token, 'jwt');
    localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(USER));

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem(STORAGE_KEYS.token)).toBeNull();
    expect(localStorage.getItem(STORAGE_KEYS.user)).toBeNull();
  });

  it('guarda y consume la ruta pendiente del acceso con Google', () => {
    expect(service.takePendingRedirect()).toBeNull();

    sessionStorage.setItem(OAUTH_REDIRECT_KEY, '/carrito');

    expect(service.takePendingRedirect()).toBe('/carrito');
    expect(sessionStorage.getItem(OAUTH_REDIRECT_KEY)).toBeNull();
  });

  it('inicia el flujo de Google con la URL del backend', () => {
    const serviceWithNavigation = service as unknown as { navigateTo: (url: string) => void };
    const navigate = vi.spyOn(serviceWithNavigation, 'navigateTo').mockImplementation(() => undefined);

    service.loginWithGoogle('/cuenta');

    expect(navigate).toHaveBeenCalledWith(GOOGLE_AUTH_URL);
    expect(sessionStorage.getItem(OAUTH_REDIRECT_KEY)).toBe('/cuenta');
  });

  it('reconoce los roles de administrador y vendedor', () => {
    service.login({ email: USER.email, password: 'secreta' }).subscribe();
    httpMock.expectOne(`${AUTH_URL}/login`)
      .flush({ token: 'jwt-admin', user: { ...USER, role: 'ADMIN' } });

    expect(service.isAdmin()).toBe(true);
    expect(service.isSeller()).toBe(false);
  });
});
