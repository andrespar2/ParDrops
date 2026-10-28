/**
 * @file src/app/pages/login/login.component.spec.ts
 * @description Pruebas del botón y los mensajes del acceso con Google.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/services/auth.service';

describe('LoginComponent', () => {
  const authStub = {
    login: vi.fn(),
    register: vi.fn(),
    loginWithGoogle: vi.fn()
  };

  async function setup(params: Record<string, string> = {}): Promise<ComponentFixture<LoginComponent>> {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(params) } }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    authStub.login.mockReset();
    authStub.register.mockReset();
    authStub.loginWithGoogle.mockReset();
  });

  it('muestra el botón de Google en el modo de inicio de sesión', async () => {
    const fixture = await setup();

    const button = fixture.nativeElement.querySelector('.auth__google') as HTMLElement;
    expect(button).toBeTruthy();
    expect(button.textContent).toContain('Continuar con Google');
    expect(button.querySelector('svg')).toBeTruthy();
  });

  it('inicia el flujo de Google conservando la ruta solicitada', async () => {
    const fixture = await setup({ redirect: '/carrito' });

    fixture.componentInstance.onGoogleLogin();

    expect(authStub.loginWithGoogle).toHaveBeenCalledWith('/carrito');
  });

  it('muestra el mensaje cuando Google cancela el acceso', async () => {
    const fixture = await setup({ oauth: 'cancelado' });

    expect(fixture.componentInstance.errorMessage()).toBe('Cancelaste el acceso con Google.');
  });

  it('muestra un mensaje para una cuenta suspendida', async () => {
    const fixture = await setup({ oauth: 'cuenta-suspendida' });

    expect(fixture.componentInstance.errorMessage()).toContain('Tu cuenta está suspendida');
  });

  it('mantiene el formulario de inicio de sesión', async () => {
    const fixture = await setup();
    authStub.login.mockReturnValue({ subscribe: () => undefined });

    fixture.componentInstance.loginForm.setValue({
      email: 'cliente@pardrops.com',
      password: 'secreta'
    });
    fixture.componentInstance.onLogin();

    expect(authStub.login).toHaveBeenCalledWith({
      email: 'cliente@pardrops.com',
      password: 'secreta'
    });
  });
});
