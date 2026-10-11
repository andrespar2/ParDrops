/**
 * @file src/app/shared/components/header/header.component.ts
 * @description Componente de cabecera para la navegación principal.
 * 
 * Este componente muestra la marca/logo, el menú de navegación,
 * el buscador y el indicador del carrito de compras con el contador
 * de ítems.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
  /** Servicio del carrito para obtener el contador de ítems */
  private readonly cartService = inject(CartService);

  /** Contador computado de ítems en el carrito */
  public readonly itemCount = computed(() => this.cartService.itemCount());

  /** Estado del menú móvil (expandido/colapsado) */
  public isMobileMenuOpen = false;

  /**
   * Alterna el estado del menú móvil.
   * @returns {void}
   */
  public toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  /**
   * Cierra el menú móvil.
   * @returns {void}
   */
  public closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }
}