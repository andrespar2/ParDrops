/**
 * @file src/app/app.config.ts
 * @description Configuración global de la aplicación.
 *
 * Registra el router, la hidratación de cliente y el cliente HTTP con el
 * interceptor que adjunta el token de sesión a las llamadas a la API.
 */

import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideClientHydration(),
    // Cliente HTTP basado en fetch con el interceptor de autenticación.
    provideHttpClient(withFetch(), withInterceptors([authInterceptor]))
  ]
};
