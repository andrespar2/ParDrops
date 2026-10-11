/**
 * @file src/app/core/guards/admin.guard.ts
 * @description Guarda de ruta que exige el rol de administrador.
 *
 * Deja pasar solo a los usuarios con rol ADMIN; cualquier otro visitante es
 * redirigido a la página de inicio.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAdmin()) {
    return true;
  }

  return router.createUrlTree(['/']);
};
