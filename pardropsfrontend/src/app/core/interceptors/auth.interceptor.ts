/**
 * @file src/app/core/interceptors/auth.interceptor.ts
 * @description Interceptor HTTP que adjunta el token JWT a cada petición.
 *
 * Si hay una sesión activa, añade la cabecera `Authorization: Bearer <token>`
 * a todas las llamadas a la API. También, si el servidor responde con 401,
 * cierra la sesión local para evitar mantener un token inválido.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const token = authService.token();

  // Solo añadimos el token a las llamadas dirigidas a nuestra API.
  const isApiRequest = request.url.startsWith(API_BASE_URL);

  const authorizedRequest = token && isApiRequest
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authorizedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      // Token caducado o inválido: se limpia la sesión para forzar un nuevo login.
      if (error.status === 401 && isApiRequest) {
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};
