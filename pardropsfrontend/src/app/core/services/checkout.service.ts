/**
 * @file src/app/core/services/checkout.service.ts
 * @description Llamadas a la API de checkout con Wompi.
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { CreateOrderRequest } from '../models/order.model';
import { CheckoutSession, CheckoutStatus } from '../models/checkout.model';

@Injectable({
  providedIn: 'root'
})
export class CheckoutService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** URL base del checkout. */
  private readonly checkoutUrl = `${API_BASE_URL}/checkout`;

  /**
   * Crea un pedido pendiente y prepara el widget de Wompi.
   *
   * @param request datos del comprador y del carrito.
   * @returns observable con los datos firmados del checkout.
   */
  public createSession(request: CreateOrderRequest): Observable<CheckoutSession> {
    return this.http.post<CheckoutSession>(this.checkoutUrl, request);
  }

  /**
   * Consulta el estado de un pago.
   *
   * @param reference     referencia del pago.
   * @param transactionId identificador de Wompi de la URL de retorno.
   * @returns observable con el estado verificado.
   */
  public getStatus(reference: string, transactionId?: string | null): Observable<CheckoutStatus> {
    let params = new HttpParams();
    if (transactionId) {
      params = params.set('transactionId', transactionId);
    }
    return this.http.get<CheckoutStatus>(
      `${this.checkoutUrl}/${encodeURIComponent(reference)}`,
      { params }
    );
  }

  /**
   * Regenera la referencia para reintentar un pago no aprobado.
   *
   * @param reference referencia actual.
   * @returns observable con el nuevo intento.
   */
  public retry(reference: string): Observable<CheckoutSession> {
    return this.http.post<CheckoutSession>(
      `${this.checkoutUrl}/${encodeURIComponent(reference)}/retry`,
      {}
    );
  }
}
