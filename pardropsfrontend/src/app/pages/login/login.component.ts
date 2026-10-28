/**
 * @file src/app/pages/login/login.component.ts
 * @description Página de autenticación: inicio de sesión y registro.
 *
 * Muestra un formulario que alterna entre "Iniciar sesión" y "Crear cuenta".
 * Al autenticarse correctamente, redirige a la ruta solicitada antes de entrar
 * (parámetro `redirect`) o a la página de la cuenta. También ofrece la opción
 * de continuar navegando como invitado.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

/**
 * Valida que las contraseñas de los dos campos coincidan.
 *
 * @param control grupo del formulario con `password` y `confirmPassword`.
 * @returns `null` si coinciden o un error si no.
 */
function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password === confirm ? null : { passwordsMismatch: true };
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  /** Modo actual del formulario: 'login' o 'register'. */
  public readonly mode = signal<'login' | 'register'>('login');

  /** Indica si hay una operación en curso. */
  public readonly loading = signal<boolean>(false);

  /** Mensaje de error a mostrar (si lo hubiera). */
  public readonly errorMessage = signal<string | null>(null);

  /** Indica si el modo actual es el de registro. */
  public readonly isRegisterMode = computed(() => this.mode() === 'register');

  /** Formulario de inicio de sesión. */
  public readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  /** Formulario de registro. */
  public readonly registerForm = this.fb.nonNullable.group(
    {
      fullName: ['', [Validators.required, Validators.maxLength(120)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.maxLength(30)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: passwordsMatch }
  );

  /**
   * Cambia entre los modos de login y registro.
   *
   * @param mode modo al que cambiar.
   * @returns {void}
   */
  public setMode(mode: 'login' | 'register'): void {
    this.mode.set(mode);
    this.errorMessage.set(null);
  }

  /**
   * Procesa el inicio de sesión.
   *
   * @returns {void}
   */
  public onLogin(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: () => this.redirectAfterAuth(),
      error: (error: HttpErrorResponse) => this.handleError(error),
      complete: () => this.loading.set(false)
    });
  }

  /**
   * Procesa el registro de una cuenta nueva.
   *
   * @returns {void}
   */
  public onRegister(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const { fullName, email, phone, password } = this.registerForm.getRawValue();

    this.authService.register({ fullName, email, phone, password }).subscribe({
      next: () => this.redirectAfterAuth(),
      error: (error: HttpErrorResponse) => this.handleError(error),
      complete: () => this.loading.set(false)
    });
  }

  /**
   * Inicia el flujo de acceso con Google.
   *
   * @returns {void}
   */
  public onGoogleLogin(): void {
    this.errorMessage.set(null);
    this.authService.loginWithGoogle();
  }

  /**
   * Vuelve a la ruta desde la que se llegó al login, o a la cuenta.
   *
   * @returns {void}
   * @private
   */
  private redirectAfterAuth(): void {
    const redirect = this.route.snapshot.queryParamMap.get('redirect');
    this.router.navigateByUrl(redirect || '/cuenta');
  }

  /**
   * Extrae un mensaje legible del error devuelto por la API.
   *
   * @param error error HTTP recibido.
   * @returns {void}
   * @private
   */
  private handleError(error: HttpErrorResponse): void {
    const apiMessage = error.error?.message;
    this.errorMessage.set(apiMessage || 'No se pudo completar la operación. Inténtalo de nuevo.');
    this.loading.set(false);
  }
}
