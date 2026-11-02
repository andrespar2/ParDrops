/**
 * @file src/app/shared/components/product-card/product-card.component.ts
 * @description Tarjeta de producto: imagen, precio, vendedor y favoritos.
 */

import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Product } from '../../../core/models/product.model';
import { CartService } from '../../../core/services/cart.service';
import { FavoritesService } from '../../../core/services/favorites.service';
import { formatCurrency } from '../../../core/config/commerce.config';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product-card.component.html',
  styleUrls: ['./product-card.component.scss']
})
export class ProductCardComponent {
  /** Producto a mostrar. */
  @Input({ required: true }) product!: Product;

  /** Servicio del carrito. */
  private readonly cartService = inject(CartService);

  /** Favoritos del visitante. */
  private readonly favoritesService = inject(FavoritesService);

  /**
   * Imagen principal del producto, o la primera disponible.
   */
  public getPrimaryImage(): string {
    const primary = this.product.images.find(img => img.isPrimary);
    return primary?.url || this.product.images[0]?.url || '';
  }

  /** Indica si el producto está guardado en favoritos. */
  public isFavorite(): boolean {
    return this.favoritesService.isFavorite(this.product.id);
  }

  /**
   * Añade o quita el producto de favoritos sin abrir el detalle.
   *
   * @param event evento del clic.
   */
  public toggleFavorite(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.favoritesService.toggle(this.product.id);
  }

  /** Añade una unidad del producto al carrito. */
  public addToCart(): void {
    this.cartService.addItem(this.product, 1);
  }

  /**
   * Formatea un importe en pesos colombianos.
   *
   * @param price importe.
   */
  public formatPrice(price: number): string {
    return formatCurrency(price);
  }

  /** Texto de disponibilidad según el stock. */
  public get availability(): string {
    if (this.product.stock === 0) {
      return 'Agotado';
    }
    if (this.product.stock <= 3) {
      return `Últimas ${this.product.stock} unidades`;
    }
    return 'Disponible';
  }
}
