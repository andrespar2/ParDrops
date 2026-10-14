/**
 * @file src/app/core/services/review.service.ts
 * @description Servicio para consultar y publicar reseñas de productos.
 *
 * El backend decide qué usuarios pueden reseñar (clientes con el pedido
 * entregado y sin reseña previa); el frontend solo consulta ese estado y
 * muestra el formulario cuando la API lo permite.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { CreateReviewRequest, ProductReview, ReviewEligibility } from '../models/review.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** URL base de las reseñas de la API. */
  private readonly reviewsUrl = `${API_BASE_URL}/products`;

  /**
   * Obtiene las reseñas publicadas sobre un producto.
   *
   * @param productId identificador del producto.
   * @returns observable con las reseñas, de la más reciente a la más antigua.
   */
  public getReviews(productId: string): Observable<ProductReview[]> {
    return this.http.get<ProductReview[]>(`${this.reviewsUrl}/${productId}/reviews`);
  }

  /**
   * Consulta si el usuario actual puede reseñar el producto.
   *
   * @param productId identificador del producto.
   * @returns observable con el estado de elegibilidad.
   */
  public getEligibility(productId: string): Observable<ReviewEligibility> {
    return this.http.get<ReviewEligibility>(
      `${this.reviewsUrl}/${productId}/reviews/eligibility`
    );
  }

  /**
   * Publica una reseña con la sesión activa.
   *
   * @param productId identificador del producto.
   * @param request   calificación y comentario.
   * @returns observable con la reseña creada.
   */
  public createReview(productId: string, request: CreateReviewRequest): Observable<ProductReview> {
    return this.http.post<ProductReview>(
      `${this.reviewsUrl}/${productId}/reviews`,
      request
    );
  }
}
