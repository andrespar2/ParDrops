/**
 * @file src/app/core/guards/seller.guard.ts
 * @description Guarda de ruta que exige el rol de vendedor.
 *
 * Deja pasar solo a los usuarios con rol SELLER; cualquier otro visitante es
 * redirigido a la página de inicio. El backend vuelve a comprobar el rol, así
 * que la protección real no depende del cliente.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const sellerGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isSeller()) {
    return true;
  }

  return router.createUrlTree(['/']);
};
