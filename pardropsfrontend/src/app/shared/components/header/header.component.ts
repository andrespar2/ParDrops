/**
 * @file src/app/shared/components/header/header.component.ts
 * @description Componente de cabecera para la navegación principal.
 *
 * Muestra la marca/logo, el menú de navegación, el buscador, el acceso a la
 * cuenta del usuario y el indicador del carrito de compras con el contador de
 * ítems. Para el administrador incluye además el aviso de pedidos por gestionar.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { OrderService } from '../../../core/services/order.service';
import { NotificationBellComponent } from '../notification-bell/notification-bell.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, NotificationBellComponent],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
  /** Servicio del carrito para obtener el contador de ítems. */
  private readonly cartService = inject(CartService);

  /** Servicio de autenticación para conocer el estado de la sesión. */
  private readonly authService = inject(AuthService);

  /** Servicio de pedidos para avisar de las compras nuevas. */
  private readonly orderService = inject(OrderService);

  /** Router para redirigir tras cerrar sesión. */
  private readonly router = inject(Router);

  /** Referencia de destrucción para detener el sondeo de pedidos. */
  private readonly destroyRef = inject(DestroyRef);

  /** Contador computado de ítems en el carrito. */
  public readonly itemCount = computed(() => this.cartService.itemCount());

  /** Pedidos pendientes de gestión (aviso para el administrador). */
  public readonly pendingCount = this.orderService.pendingCount;

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

  constructor() {
    // Mientras haya una sesión de administrador, se vigilan los pedidos nuevos.
    effect(() => {
      if (this.authService.isAdmin()) {
        this.orderService.startNotificationPolling();
      } else {
        this.orderService.stopNotificationPolling();
      }
    });

    this.destroyRef.onDestroy(() => this.orderService.stopNotificationPolling());
  }

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
