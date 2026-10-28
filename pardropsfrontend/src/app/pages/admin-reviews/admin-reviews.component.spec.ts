/**
 * @file src/app/pages/admin-reviews/admin-reviews.component.spec.ts
 * @description Pruebas de la moderación de reseñas.
 */

import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminReviewsComponent } from './admin-reviews.component';
import { AdminService } from '../../core/services/admin.service';
import { AdminReview } from '../../core/models/admin.model';

const REVIEWS: AdminReview[] = [
  {
    id: 1,
    productId: 'p1',
    productName: 'Camiseta',
    rating: 5,
    comment: 'Excelente',
    customerName: 'Ana',
    images: ['/media/a.jpg'],
    createdAt: '2026-10-28T10:00:00Z'
  },
  {
    id: 2,
    productId: 'p2',
    productName: 'Gorra',
    rating: 3,
    comment: 'Regular',
    customerName: 'Luis',
    images: [],
    createdAt: '2026-10-28T09:00:00Z'
  }
];

describe('AdminReviewsComponent', () => {
  const adminStub = {
    listReviews: vi.fn(() => of(REVIEWS)),
    deleteReview: vi.fn(() => of(void 0))
  };

  beforeEach(() => {
    adminStub.listReviews.mockClear();
    adminStub.deleteReview.mockClear();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });

  async function setup(): Promise<AdminReviewsComponent> {
    await TestBed.configureTestingModule({
      imports: [AdminReviewsComponent],
      providers: [{ provide: AdminService, useValue: adminStub }]
    }).compileComponents();

    const fixture = TestBed.createComponent(AdminReviewsComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('carga las reseñas', async () => {
    const component = await setup();

    expect(component.reviews().length).toBe(2);
    expect(component.loading()).toBe(false);
  });

  it('elimina una reseña y la quita de la lista', async () => {
    const component = await setup();

    component.remove(REVIEWS[0]);

    expect(adminStub.deleteReview).toHaveBeenCalledWith(1);
    expect(component.reviews().length).toBe(1);
    expect(component.okMessage()).toContain('eliminada');
  });
});
