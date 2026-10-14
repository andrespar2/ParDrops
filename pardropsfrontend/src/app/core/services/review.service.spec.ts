/**
 * @file src/app/core/services/review.service.spec.ts
 * @description Pruebas del servicio de reseñas de productos.
 */

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ReviewService } from './review.service';
import { ProductReview } from '../models/review.model';

const PRODUCT_ID = 'producto-1';
const REVIEWS_URL = `http://localhost:8080/api/products/${PRODUCT_ID}/reviews`;

/**
 * Crea una reseña de prueba.
 *
 * @param id identificador de la reseña.
 * @returns la reseña lista para usar en las pruebas.
 */
function buildReview(id: number): ProductReview {
  return {
    id,
    rating: 5,
    comment: 'Muy buen producto.',
    customerName: 'Cliente de Prueba',
    createdAt: '2026-01-01T10:00:00Z'
  };
}

describe('ReviewService', () => {
  let service: ReviewService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(ReviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('obtiene las reseñas de un producto por su identificador', () => {
    let reviews: ProductReview[] | undefined;
    service.getReviews(PRODUCT_ID).subscribe(result => (reviews = result));

    const request = httpMock.expectOne(REVIEWS_URL);
    expect(request.request.method).toBe('GET');

    request.flush([buildReview(1)]);

    expect(reviews?.length).toBe(1);
    expect(reviews?.[0].comment).toBe('Muy buen producto.');
  });

  it('consulta si el usuario puede reseñar el producto', () => {
    let canReview: boolean | undefined;
    service.getEligibility(PRODUCT_ID).subscribe(result => (canReview = result.canReview));

    const request = httpMock.expectOne(`${REVIEWS_URL}/eligibility`);
    expect(request.request.method).toBe('GET');

    request.flush({ canReview: true, hasReviewed: false });

    expect(canReview).toBe(true);
  });

  it('publica una reseña con la calificación y el comentario', () => {
    service.createReview(PRODUCT_ID, { rating: 4, comment: 'Bastante bien.' }).subscribe();

    const request = httpMock.expectOne(REVIEWS_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ rating: 4, comment: 'Bastante bien.' });

    request.flush(buildReview(2));
  });
});
