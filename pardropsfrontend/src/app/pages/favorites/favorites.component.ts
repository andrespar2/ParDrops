/**
 * @file src/app/pages/favorites/favorites.component.ts
 * @description Productos guardados por el visitante.
 */

import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { FavoritesService } from '../../core/services/favorites.service';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-favorites',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent],
  templateUrl: './favorites.component.html',
  styleUrls: ['./favorites.component.scss']
})
export class FavoritesComponent {
  /** Catálogo completo. */
  private readonly productService = inject(ProductService);

  /** Favoritos guardados. */
  private readonly favoritesService = inject(FavoritesService);

  /** Productos favoritos, resueltos desde el catálogo. */
  public readonly products = computed(() => {
    const ids = new Set(this.favoritesService.ids());
    return this.productService.products().filter(product => ids.has(product.id));
  });

  /** Indica si el catálogo todavía se está cargando. */
  public readonly loading = this.productService.loading;
}
