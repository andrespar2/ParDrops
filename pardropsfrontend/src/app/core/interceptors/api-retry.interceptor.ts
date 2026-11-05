/**
 * @file src/app/core/interceptors/api-retry.interceptor.ts
 * @description Reintentos controlados para peticiones GET a la API.
 *
 * Render Free duerme el backend tras un periodo de inactividad. La primera
 * petición puede devolver 502/503/504 o fallar a nivel de red mientras el
 * servicio despierta. Este interceptor reintenta solo lecturas y solo ante
 * errores transitorios, nunca ante 401/403/404.
 */

import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize, throwError, timer } from 'rxjs';
import { retry, timeout } from 'rxjs/operators';
import { API_BASE_URL } from '../config/api.config';
import { ConnectionStatusService } from '../services/connection-status.service';

/** Intentos adicionales después del primero. */
const MAX_RETRIES = 5;

/** Tiempo máximo por intento antes de considerarlo fallido. */
const ATTEMPT_TIMEOUT_MS = 30_000;

/** Esperas entre intentos (2 s, 4 s, 8 s, 15 s, 25 s). */
const DELAYS_MS = [2_000, 4_000, 8_000, 15_000, 25_000];

export const apiRetryInterceptor: HttpInterceptorFn = (request, next) => {
  const isApiGet = request.method === 'GET' && request.url.startsWith(API_BASE_URL);
  if (!isApiGet) {
    return next(request);
  }

  const status = inject(ConnectionStatusService);
  let retrying = false;

  const markRetry = () => {
    if (!retrying) {
      retrying = true;
      status.beginRetry();
    }
  };

  return next(request).pipe(
    timeout(ATTEMPT_TIMEOUT_MS),
    retry({
      count: MAX_RETRIES,
      delay: (error: unknown, retryCount: number) => {
        if (!isTransient(error)) {
          return throwError(() => error);
        }
        markRetry();
        const delay = DELAYS_MS[Math.min(retryCount - 1, DELAYS_MS.length - 1)];
        return timer(delay);
      }
    }),
    finalize(() => {
      if (retrying) {
        status.endRetry();
      }
    })
  );
};

function isTransient(error: unknown): boolean {
  if (error instanceof HttpErrorResponse) {
    return error.status === 0
      || error.status === 429
      || error.status === 502
      || error.status === 503
      || error.status === 504;
  }
  // TimeoutError de RxJS: la API no respondió dentro del margen del intento.
  return error instanceof Error && error.name === 'TimeoutError';
}
