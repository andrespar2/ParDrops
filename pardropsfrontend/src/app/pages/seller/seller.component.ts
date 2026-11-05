/**
 * @file src/app/pages/seller/seller.component.ts
 * @description Armazón del panel privado del vendedor.
 *
 * Envuelve las secciones del panel (resumen, productos, pedidos y perfil de la
 * tienda) con una navegación lateral. Solo es accesible con rol SELLER
 * (protegida por `sellerGuard` y, en el servidor, por el rol del JWT).
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

/**
 * Sección del panel del vendedor.
 *
 * @interface SellerSection
 */
interface SellerSection {
  /** Ruta relativa de la sección. */
  path: string;
  /** Etiqueta visible. */
  label: string;
  /** Descripción corta. */
  description: string;
  /** Icono SVG de la sección. */
  icon: string;
}

@Component({
  selector: 'app-seller',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './seller.component.html',
  styleUrls: ['./seller.component.scss']
})
export class SellerComponent {
  private readonly authService = inject(AuthService);

  /** Indica si el menú lateral está abierto en móvil. */
  public readonly menuOpen = signal<boolean>(false);

  /** Nombre del vendedor autenticado. */
  public readonly userName = computed(
    () => this.authService.user()?.fullName ?? 'Vendedor');

  /** Secciones disponibles del panel. */
  public readonly sections: SellerSection[] = [
    {
      path: 'resumen',
      label: 'Resumen',
      description: 'Ventas, pedidos e inventario',
      icon: 'M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z'
    },
    {
      path: 'productos',
      label: 'Mis productos',
      description: 'Publica y edita tu catálogo',
      icon: 'M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16zM3.27 6.96L12 12l8.73-5.04M12 22V12'
    },
    {
      path: 'pedidos',
      label: 'Pedidos',
      description: 'Prepara tus subpedidos',
      icon: 'M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4zM3 6h18M16 10a4 4 0 0 1-8 0'
    },
    {
      path: 'resenas',
      label: 'Reseñas',
      description: 'Calificaciones de tus productos',
      icon: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z'
    },
    {
      path: 'saldo',
      label: 'Mi saldo',
      description: 'Ganancias, movimientos y retiros',
      icon: 'M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6'
    },
    {
      path: 'perfil',
      label: 'Mi tienda',
      description: 'Ficha, logo, portada y contacto',
      icon: 'M3 9l1-5h16l1 5M4 9v11a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V9M4 9h16M9 21v-6h6v6'
    }
  ];

  /**
   * Abre o cierra el menú lateral en móvil.
   *
   * @returns {void}
   */
  public toggleMenu(): void {
    this.menuOpen.update(open => !open);
  }

  /**
   * Cierra el menú lateral tras elegir una sección.
   *
   * @returns {void}
   */
  public closeMenu(): void {
    this.menuOpen.set(false);
  }
}
