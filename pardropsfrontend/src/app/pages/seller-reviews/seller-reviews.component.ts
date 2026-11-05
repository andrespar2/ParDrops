/**
 * @file src/app/pages/seller-reviews/seller-reviews.component.ts
 * @description Consulta de calificaciones y reseñas del vendedor.
 *
 * La vista es estrictamente de solo lectura: no existen botones ni llamadas
 * para editar o borrar reseñas. La autorización real la aplica el backend.
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { SellerService } from '../../core/services/seller.service';
import { SellerReview } from '../../core/models/seller.model';
import { formatDateTime } from '../../core/config/commerce.config';

@Component({
  selector: 'app-seller-reviews',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './seller-reviews.component.html',
  styleUrls: ['./seller-reviews.component.scss']
})
export class SellerReviewsComponent {
  private readonly sellerService = inject(SellerService);

  /** Reseñas recibidas. */
  public readonly reviews = signal<SellerReview[]>([]);

  /** Indica si se están cargando. */
  public readonly loading = signal<boolean>(true);

  /** Error de carga. */
  public readonly errorMessage = signal<string | null>(null);

  /** Filtro por estrellas (0 = todas). */
  public ratingFilter = 0;

  /** Media de las reseñas cargadas. */
  public readonly average = computed(() => {
    const values = this.reviews();
    if (values.length === 0) return 0;
    return Math.round(
      (values.reduce((sum, review) => sum + review.rating, 0) / values.length) * 100
    ) / 100;
  });

  /** Reseñas que cumplen el filtro. */
  public readonly filtered = computed(() => {
    const rating = this.ratingFilter;
    return rating === 0
      ? this.reviews()
      : this.reviews().filter(review => review.rating === rating);
  });

  constructor() {
    this.load();
  }

  /** Carga las reseñas del vendedor autenticado. */
  public load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.sellerService.listReviews().subscribe({
      next: reviews => {
        this.reviews.set(reviews);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudieron cargar tus reseñas.'
        );
        this.loading.set(false);
      }
    });
  }

  /** Formatea una fecha con hora. */
  public formatDate(value: string): string {
    return formatDateTime(value);
  }

  /** Texto de estrellas para accesibilidad. */
  public stars(rating: number): string {
    return '★'.repeat(rating) + '☆'.repeat(Math.max(0, 5 - rating));
  }
}
