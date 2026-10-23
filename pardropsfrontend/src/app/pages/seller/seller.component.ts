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
      description: 'Ventas, pedidos e inventario'
    },
    {
      path: 'productos',
      label: 'Mis productos',
      description: 'Publica y edita tu catálogo'
    },
    {
      path: 'pedidos',
      label: 'Pedidos',
      description: 'Prepara tus subpedidos'
    },
    {
      path: 'perfil',
      label: 'Mi tienda',
      description: 'Ficha, logo, portada y contacto'
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
