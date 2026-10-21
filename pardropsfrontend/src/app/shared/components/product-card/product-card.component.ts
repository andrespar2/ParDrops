/**
 * @file src/app/shared/components/product-card/product-card.component.ts
 * @description Componente de tarjeta para mostrar productos en listados.
 * 
 * Muestra información esencial del producto: imagen, nombre, precio,
 * etiquetas (nuevo, oferta) y botón para añadir al carrito.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Product } from '../../../core/models/product.model';
import { CartService } from '../../../core/services/cart.service';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product-card.component.html',
  styleUrls: ['./product-card.component.scss']
})
export class ProductCardComponent {
  /** Producto a mostrar */
  @Input({ required: true }) product!: Product;

  /** Servicio del carrito */
  private readonly cartService = inject(CartService);

  /**
   * Obtiene la imagen principal del producto.
   * @returns {string} URL de la imagen principal o primera imagen.
   */
  public getPrimaryImage(): string {
    const primary = this.product.images.find(img => img.isPrimary);
    return primary?.url || this.product.images[0]?.url || '';
  }

  /**
   * Añade el producto al carrito con cantidad 1.
   * @returns {void}
   */
  public addToCart(): void {
    this.cartService.addItem(this.product, 1);
  }

  /**
   * Formatea el precio en pesos colombianos (COP).
   *
   * Si el producto no trae moneda, se asume COP: la plataforma opera en
   * Colombia y ningún precio debe mostrarse en otra divisa.
   *
   * @param {number} price - Precio a formatear.
   * @returns {string} Precio formateado (ej. "$ 119.900,00").
   */
  public formatPrice(price: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: this.product.currency || 'COP',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(price);
  }
}