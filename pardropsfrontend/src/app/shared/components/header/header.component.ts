/**
 * @file src/app/shared/components/header/header.component.ts
 * @description Cabecera principal: marca, buscador, accesos y sesión.
 *
 * Mantiene el menú móvil, el desplegable de usuario, el contador del carrito,
 * las notificaciones y el aviso de pedidos pendientes para administración.
 * Añade el buscador global, los favoritos y el selector de tema.
 */

import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { OrderService } from '../../../core/services/order.service';
import { FavoritesService } from '../../../core/services/favorites.service';
import { ThemeService } from '../../../core/services/theme.service';
import { NotificationBellComponent } from '../notification-bell/notification-bell.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, NotificationBellComponent],
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

  /** Favoritos guardados en el navegador. */
  private readonly favoritesService = inject(FavoritesService);

  /** Preferencia de tema. */
  private readonly themeService = inject(ThemeService);

  /** Router para redirigir tras buscar o cerrar sesión. */
  private readonly router = inject(Router);

  /** Referencia de destrucción para detener el sondeo de pedidos. */
  private readonly destroyRef = inject(DestroyRef);

  /** Contador computado de ítems en el carrito. */
  public readonly itemCount = computed(() => this.cartService.itemCount());

  /** Número de favoritos guardados. */
  public readonly favoritesCount = this.favoritesService.count;

  /** Pedidos pendientes de gestión (aviso para el administrador). */
  public readonly pendingCount = this.orderService.pendingCount;

  /** Usuario autenticado (o nulo). */
  public readonly user = this.authService.user;

  /** Indica si hay una sesión iniciada. */
  public readonly isAuthenticated = this.authService.isAuthenticated;

  /** Indica si el usuario es administrador. */
  public readonly isAdmin = this.authService.isAdmin;

  /** Indica si el usuario es vendedor. */
  public readonly isSeller = this.authService.isSeller;

  /** Texto del buscador. */
  public readonly searchTerm = signal<string>('');

  /** Etiqueta accesible del tema actual. */
  public readonly themeLabel = computed(() => this.themeService.label());

  /** Modo de tema actual, usado para elegir el icono. */
  public readonly themeMode = this.themeService.mode;

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
   * Envía la búsqueda al catálogo.
   */
  public search(): void {
    const term = this.searchTerm().trim();
    this.router.navigate(['/productos'], {
      queryParams: term ? { search: term } : {}
    });
    this.closeMobileMenu();
  }

  /** Cambia entre tema automático, claro y oscuro. */
  public toggleTheme(): void {
    this.themeService.cycle();
  }

  /**
   * Alterna el estado del menú móvil.
   */
  public toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  /**
   * Cierra el menú móvil.
   */
  public closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  /**
   * Alterna el desplegable del usuario.
   */
  public toggleUserMenu(): void {
    this.isUserMenuOpen.update(open => !open);
  }

  /**
   * Cierra la sesión, cierra los menús y vuelve al inicio.
   */
  public logout(): void {
    this.authService.logout();
    this.isUserMenuOpen.set(false);
    this.isMobileMenuOpen = false;
    this.router.navigate(['/']);
  }
}
