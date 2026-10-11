/**
 * @file src/app/shared/components/header/header.component.ts
 * @description Componente de cabecera para la navegación principal.
 *
 * Muestra la marca/logo, el menú de navegación, el buscador, el acceso a la
 * cuenta del usuario y el indicador del carrito de compras con el contador de
 * ítems.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
  /** Servicio del carrito para obtener el contador de ítems. */
  private readonly cartService = inject(CartService);

  /** Servicio de autenticación para conocer el estado de la sesión. */
  private readonly authService = inject(AuthService);

  /** Router para redirigir tras cerrar sesión. */
  private readonly router = inject(Router);

  /** Contador computado de ítems en el carrito. */
  public readonly itemCount = computed(() => this.cartService.itemCount());

  /** Usuario autenticado (o nulo). */
  public readonly user = this.authService.user;

  /** Indica si hay una sesión iniciada. */
  public readonly isAuthenticated = this.authService.isAuthenticated;

  /** Indica si el usuario es administrador. */
  public readonly isAdmin = this.authService.isAdmin;

  /** Estado del menú móvil (expandido/colapsado). */
  public isMobileMenuOpen = false;

  /** Estado del desplegable de usuario. */
  public readonly isUserMenuOpen = signal<boolean>(false);

  /**
   * Alterna el estado del menú móvil.
   *
   * @returns {void}
   */
  public toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  /**
   * Cierra el menú móvil.
   *
   * @returns {void}
   */
  public closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  /**
   * Alterna el desplegable del usuario.
   *
   * @returns {void}
   */
  public toggleUserMenu(): void {
    this.isUserMenuOpen.update(open => !open);
  }

  /**
   * Cierra la sesión, cierra los menús y vuelve al inicio.
   *
   * @returns {void}
   */
  public logout(): void {
    this.authService.logout();
    this.isUserMenuOpen.set(false);
    this.isMobileMenuOpen = false;
    this.router.navigate(['/']);
  }
}
