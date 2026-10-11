/**
 * @file src/app/core/guards/auth.guard.ts
 * @description Guarda de ruta que exige una sesión iniciada.
 *
 * Si el visitante no está autenticado, se le redirige a la página de login
 * guardando la ruta a la que quería acceder para volver a ella después.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: { redirect: state.url }
  });
};
