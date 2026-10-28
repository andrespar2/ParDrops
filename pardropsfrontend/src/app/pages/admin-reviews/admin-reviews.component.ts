/**
 * @file src/app/pages/admin-reviews/admin-reviews.component.ts
 * @description Moderación de las reseñas de la plataforma.
 */

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminService } from '../../core/services/admin.service';
import { AdminReview } from '../../core/models/admin.model';
import { StarRatingComponent } from '../../shared/components/star-rating/star-rating.component';

@Component({
  selector: 'app-admin-reviews',
  standalone: true,
  imports: [CommonModule, StarRatingComponent],
  templateUrl: './admin-reviews.component.html',
  styleUrls: ['./admin-reviews.component.scss']
})
export class AdminReviewsComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  /** Reseñas de la plataforma. */
  public readonly reviews = signal<AdminReview[]>([]);

  /** Indica si están cargando. */
  public readonly loading = signal<boolean>(true);

  /** Mensaje de error. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de éxito. */
  public readonly okMessage = signal<string | null>(null);

  public ngOnInit(): void {
    this.load();
  }

  /**
   * Carga las reseñas.
   *
   * @returns {void}
   */
  public load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.adminService.listReviews().subscribe({
      next: reviews => {
        this.reviews.set(reviews);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(error.error?.message || 'No se pudieron cargar las reseñas.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Elimina una reseña.
   *
   * @param review reseña a eliminar.
   * @returns {void}
   */
  public remove(review: AdminReview): void {
    if (typeof window !== 'undefined' && !window.confirm('¿Eliminar esta reseña?')) {
      return;
    }
    this.adminService.deleteReview(review.id).subscribe({
      next: () => {
        this.reviews.update(list => list.filter(item => item.id !== review.id));
        this.okMessage.set('Reseña eliminada y valoración del producto actualizada.');
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(error.error?.message || 'No se pudo eliminar la reseña.');
      }
    });
  }
}
