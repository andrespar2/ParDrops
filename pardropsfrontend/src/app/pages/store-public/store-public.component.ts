/**
 * @file src/app/pages/store-public/store-public.component.ts
 * @description Página pública de la tienda de un vendedor.
 *
 * Muestra la ficha comercial (portada, logotipo, descripción y contacto) y
 * solo los productos publicados de esa tienda. Si el slug no existe, se
 * muestra un estado de "tienda no encontrada".
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { StoreService } from '../../core/services/store.service';
import { Store } from '../../core/models/store.model';
import { Product } from '../../core/models/product.model';
import { ProductReview } from '../../core/models/review.model';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { StarRatingComponent } from '../../shared/components/star-rating/star-rating.component';

@Component({
  selector: 'app-store-public',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent, StarRatingComponent],
  templateUrl: './store-public.component.html',
  styleUrls: ['./store-public.component.scss']
})
export class StorePublicComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly storeService = inject(StoreService);

  /** Ficha de la tienda. */
  public readonly store = signal<Store | null>(null);

  /** Productos publicados de la tienda. */
  public readonly products = signal<Product[]>([]);

  /** Reseñas recientes de los productos de la tienda. */
  public readonly reviews = signal<ProductReview[]>([]);

  /** Indica si las reseñas se están cargando. */
  public readonly reviewsLoading = signal<boolean>(false);

  /** Indica si la página está cargándose. */
  public readonly loading = signal<boolean>(true);

  /** Indica si la tienda no existe. */
  public readonly notFound = signal<boolean>(false);

  /** Mensaje de error. */
  public readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.route.paramMap.subscribe(params => {
      const slug = params.get('slug');
      if (slug) {
        this.loadStore(slug);
      }
    });
  }

  /**
   * Carga la ficha y el catálogo publicado de una tienda.
   *
   * @param slug slug público de la tienda.
   * @returns {void}
   */
  private loadStore(slug: string): void {
    this.loading.set(true);
    this.notFound.set(false);
    this.errorMessage.set(null);
    this.store.set(null);
    this.products.set([]);
    this.reviews.set([]);

    this.storeService.getBySlug(slug).subscribe({
      next: store => {
        this.store.set(store);
        this.loadProducts(slug);
        this.loadReviews(slug);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 404) {
          this.notFound.set(true);
        } else {
          this.errorMessage.set(
            error.error?.message || 'No se pudo cargar la tienda.');
        }
        this.loading.set(false);
      }
    });
  }

  /**
   * Carga los productos publicados de la tienda.
   *
   * @param slug slug público de la tienda.
   * @returns {void}
   */
  private loadProducts(slug: string): void {
    this.storeService.getProducts(slug).subscribe({
      next: products => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.products.set([]);
        this.loading.set(false);
      }
    });
  }

  /**
   * Carga las reseñas recientes de la tienda.
   *
   * @param slug slug público de la tienda.
   * @returns {void}
   */
  private loadReviews(slug: string): void {
    this.reviewsLoading.set(true);
    this.storeService.getReviews(slug).subscribe({
      next: reviews => {
        this.reviews.set(reviews);
        this.reviewsLoading.set(false);
      },
      error: () => {
        this.reviews.set([]);
        this.reviewsLoading.set(false);
      }
    });
  }
}
