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

  /** Reseñas seleccionadas para eliminación en masa. */
  public readonly selected = signal<Set<number>>(new Set<number>());

  /** Indica si una acción en masa está en curso. */
  public readonly bulkSaving = signal<boolean>(false);

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
        this.selected.set(new Set());
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

  /** Añade o quita una reseña de la selección. */
  public toggleSelection(id: number): void {
    this.selected.update(current => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  /** Indica si una reseña está seleccionada. */
  public isSelected(id: number): boolean {
    return this.selected().has(id);
  }

  /** Indica si todas las reseñas visibles están seleccionadas. */
  public allSelected(): boolean {
    const list = this.reviews();
    return list.length > 0 && list.every(review => this.selected().has(review.id));
  }

  /** Limpia la selección. */
  public clearSelection(): void {
    this.selected.set(new Set());
  }

  /** Selecciona o deselecciona todas las reseñas visibles. */
  public toggleAll(): void {
    if (this.allSelected()) {
      this.selected.set(new Set());
      return;
    }
    this.selected.set(new Set(this.reviews().map(review => review.id)));
  }

  /** Elimina en masa las reseñas seleccionadas. */
  public bulkDelete(): void {
    const ids = [...this.selected()];
    if (ids.length === 0) return;
    if (typeof window !== 'undefined'
        && !window.confirm(`¿Eliminar ${ids.length} reseña(s) seleccionada(s)?`)) {
      return;
    }

    this.bulkSaving.set(true);
    this.errorMessage.set(null);
    this.okMessage.set(null);
    this.adminService.bulkDeleteReviews(ids).subscribe({
      next: result => {
        this.bulkSaving.set(false);
        this.okMessage.set(
          `${result.updated} reseña(s) eliminada(s) y valoraciones actualizadas.`
        );
        this.selected.set(new Set());
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.bulkSaving.set(false);
        this.errorMessage.set(
          error.error?.message || 'No se pudieron eliminar las reseñas seleccionadas.'
        );
      }
    });
  }
}
