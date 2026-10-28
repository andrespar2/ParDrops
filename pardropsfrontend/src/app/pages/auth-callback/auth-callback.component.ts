/**
 * @file src/app/pages/auth-callback/auth-callback.component.ts
 * @description Completa la sesión iniciada con Google.
 *
 * El backend redirige aquí con un código temporal. El componente lo canjea por
 * el JWT, guarda la sesión y vuelve a la ruta solicitada o a la cuenta. Si el
 * código falta, caducó o Google canceló el acceso, muestra un mensaje claro.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './auth-callback.component.html',
  styleUrls: ['./auth-callback.component.scss']
})
export class AuthCallbackComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);

  /** Indica si el canje del código sigue en curso. */
  public readonly loading = signal<boolean>(true);

  /** Mensaje de error a mostrar si el acceso no se completa. */
  public readonly errorMessage = signal<string | null>(null);

  /**
   * Lee el código de la URL y lo canjea por la sesión.
   *
   * @returns {void}
   */
  public ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const oauthError = params.get('oauth');
    if (oauthError) {
      this.fail(this.googleErrorMessage(oauthError));
      return;
    }

    const code = params.get('code');
    if (!code) {
      this.fail('No recibimos el código de acceso de Google.');
      return;
    }

    this.authService.exchangeGoogleCode(code).subscribe({
      next: () => this.finish(),
      error: (error: HttpErrorResponse) => {
        this.fail(error.error?.message
          || 'No pudimos completar el acceso con Google. Inténtalo de nuevo.');
      }
    });
  }

  /**
   * Limpia la URL y navega al destino pendiente.
   *
   * @returns {void}
   * @private
   */
  private finish(): void {
    const redirect = this.authService.takePendingRedirect();
    this.loading.set(false);
    this.location.replaceState('/auth/callback');
    this.router.navigateByUrl(redirect || '/cuenta', { replaceUrl: true });
  }

  /**
   * Muestra el error y deja la URL sin datos sensibles.
   *
   * @param message mensaje para el usuario.
   * @returns {void}
   * @private
   */
  private fail(message: string): void {
    this.loading.set(false);
    this.errorMessage.set(message);
    this.location.replaceState('/auth/callback');
  }

  /**
   * Traduce el motivo de error del backend a un mensaje legible.
   *
   * @param reason código recibido en la URL.
   * @returns el mensaje para el usuario.
   * @private
   */
  private googleErrorMessage(reason: string): string {
    switch (reason) {
      case 'cancelado':
        return 'Cancelaste el acceso con Google.';
      case 'correo-no-verificado':
        return 'Google no confirmó que el correo te pertenezca. Usa el acceso con contraseña.';
      case 'cuenta-suspendida':
        return 'Tu cuenta está suspendida. Contacta con soporte.';
      case 'sin-correo':
        return 'Google no compartió un correo utilizable.';
      case 'no-configurado':
        return 'El acceso con Google no está configurado en este momento.';
      default:
        return 'No pudimos completar el acceso con Google. Inténtalo de nuevo.';
    }
  }
}
