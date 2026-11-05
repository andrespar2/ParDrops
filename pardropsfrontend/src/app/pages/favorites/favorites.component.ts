/**
 * @file src/app/pages/favorites/favorites.component.ts
 * @description Productos guardados por el visitante.
 */

import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { FavoritesService } from '../../core/services/favorites.service';
import { Product } from '../../core/models/product.model';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-favorites',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent],
  templateUrl: './favorites.component.html',
  styleUrls: ['./favorites.component.scss']
})
export class FavoritesComponent {
  /** Servicio del catálogo, usado para resolver solo los IDs guardados. */
  private readonly productService = inject(ProductService);

  /** Favoritos guardados en el dispositivo. */
  private readonly favoritesService = inject(FavoritesService);

  /** Productos favoritos resueltos por el servidor. */
  public readonly products = signal<Product[]>([]);

  /** Indica si la consulta está en curso. */
  public readonly loading = signal<boolean>(false);

  constructor() {
    effect(() => {
      const ids = this.favoritesService.ids();
      if (ids.length === 0) {
        this.products.set([]);
        return;
      }
      this.loading.set(true);
      this.productService.fetchProductsByIds(ids).subscribe({
        next: products => {
          this.products.set(products);
          this.loading.set(false);
        },
        error: () => {
          this.products.set([]);
          this.loading.set(false);
        }
      });
    });
  }
}
