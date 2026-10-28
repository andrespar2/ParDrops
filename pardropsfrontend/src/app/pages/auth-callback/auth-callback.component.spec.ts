/**
 * @file src/app/pages/auth-callback/auth-callback.component.spec.ts
 * @description Pruebas del canje del código y de los errores del callback.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthCallbackComponent } from './auth-callback.component';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../core/models/user.model';

const USER: User = {
  id: 1,
  email: 'cliente@pardrops.com',
  fullName: 'Cliente',
  role: 'CUSTOMER',
  provider: 'GOOGLE'
};

describe('AuthCallbackComponent', () => {
  const authStub = {
    exchangeGoogleCode: vi.fn(),
    takePendingRedirect: vi.fn()
  };
  const routerStub = { navigateByUrl: vi.fn() };
  const locationStub = { replaceState: vi.fn() };

  function setup(params: Record<string, string>): ComponentFixture<AuthCallbackComponent> {
    TestBed.configureTestingModule({
      imports: [AuthCallbackComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authStub },
        { provide: Router, useValue: routerStub },
        { provide: Location, useValue: locationStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(params) } }
        }
      ]
    });

    const fixture = TestBed.createComponent(AuthCallbackComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    authStub.exchangeGoogleCode.mockReset();
    authStub.takePendingRedirect.mockReset();
    routerStub.navigateByUrl.mockReset();
    locationStub.replaceState.mockReset();
  });

  it('canjea el código, guarda la sesión y vuelve a la ruta pendiente', () => {
    authStub.exchangeGoogleCode.mockReturnValue(of(USER));
    authStub.takePendingRedirect.mockReturnValue('/carrito');

    const fixture = setup({ code: 'codigo-valido' });

    expect(authStub.exchangeGoogleCode).toHaveBeenCalledWith('codigo-valido');
    expect(locationStub.replaceState).toHaveBeenCalledWith('/auth/callback');
    expect(routerStub.navigateByUrl).toHaveBeenCalledWith('/carrito', { replaceUrl: true });
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('vuelve a la cuenta cuando no hay ruta pendiente', () => {
    authStub.exchangeGoogleCode.mockReturnValue(of(USER));
    authStub.takePendingRedirect.mockReturnValue(null);

    setup({ code: 'codigo-valido' });

    expect(routerStub.navigateByUrl).toHaveBeenCalledWith('/cuenta', { replaceUrl: true });
  });

  it('muestra un error si falta el código', () => {
    const fixture = setup({});

    expect(authStub.exchangeGoogleCode).not.toHaveBeenCalled();
    expect(fixture.componentInstance.errorMessage())
      .toBe('No recibimos el código de acceso de Google.');
  });

  it('muestra el motivo cuando Google cancela el acceso', () => {
    const fixture = setup({ oauth: 'cancelado' });

    expect(fixture.componentInstance.errorMessage()).toBe('Cancelaste el acceso con Google.');
  });

  it('muestra el mensaje del backend si el código caducó', () => {
    authStub.exchangeGoogleCode.mockReturnValue(
      throwError(() => ({ error: { message: 'El código de acceso no es válido o caducó' } }))
    );

    const fixture = setup({ code: 'codigo-caducado' });

    expect(fixture.componentInstance.errorMessage())
      .toBe('El código de acceso no es válido o caducó');
    expect(fixture.componentInstance.loading()).toBe(false);
  });
});
