/**
 * @file src/app/core/guards/seller.guard.spec.ts
 * @description Pruebas de la guarda de ruta del panel del vendedor.
 */

import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { sellerGuard } from './seller.guard';
import { AuthService } from '../services/auth.service';

/**
 * Monta la guarda con un rol y un router de prueba.
 *
 * @param isSeller indica si el usuario simulado es vendedor.
 * @returns la guarda y el router espía.
 */
function setup(isSeller: boolean) {
  const router = {
    createUrlTree: (commands: unknown[]) => ({ commands } as unknown as UrlTree)
  };

  TestBed.configureTestingModule({
    providers: [
      { provide: AuthService, useValue: { isSeller: () => isSeller } },
      { provide: Router, useValue: router }
    ]
  });

  return { router };
}

describe('sellerGuard', () => {
  it('deja pasar al vendedor autenticado', () => {
    setup(true);

    const result = TestBed.runInInjectionContext(() => sellerGuard({} as never, {} as never));

    expect(result).toBe(true);
  });

  it('redirige al inicio cuando el visitante no es vendedor', () => {
    const { router } = setup(false);

    const result = TestBed.runInInjectionContext(() => sellerGuard({} as never, {} as never));

    expect(result).toEqual(router.createUrlTree(['/']));
  });
});
