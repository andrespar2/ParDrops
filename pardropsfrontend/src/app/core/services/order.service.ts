/**
 * @file src/app/core/services/order.service.ts
 * @description Servicio para crear y consultar pedidos.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { CreateOrderRequest, Order, OrderStatus } from '../models/order.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** URL base de los endpoints de pedidos. */
  private readonly ordersUrl = `${API_BASE_URL}/orders`;

  /** Signal con el historial del usuario autenticado. */
  private readonly _myOrders = signal<Order[]>([]);

  /** Signal con todos los pedidos (vista de administración). */
  private readonly _allOrders = signal<Order[]>([]);

  /** Historial de pedidos del usuario (solo lectura). */
  public readonly myOrders = this._myOrders.asReadonly();

  /** Todos los pedidos, para el panel de administración (solo lectura). */
  public readonly allOrders = this._allOrders.asReadonly();

  /**
   * Registra un pedido nuevo.
   *
   * @param request datos del checkout.
   * @returns observable con el pedido creado.
   */
  public createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.ordersUrl, request);
  }

  /**
   * Recupera el historial de pedidos del usuario autenticado y lo cachea.
   *
   * @returns observable con la lista de pedidos.
   */
  public loadMyOrders(): Observable<Order[]> {
    return this.http
      .get<Order[]>(`${this.ordersUrl}/me`)
      .pipe(tap(orders => this._myOrders.set(orders)));
  }

  /**
   * Recupera todos los pedidos (solo administrador).
   *
   * @returns observable con la lista completa de pedidos.
   */
  public getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(this.ordersUrl);
  }

  /**
   * Carga todos los pedidos en la signal local (uso del panel de administración).
   *
   * @returns observable con la lista completa de pedidos.
   */
  public loadAllOrders(): Observable<Order[]> {
    return this.http
      .get<Order[]>(this.ordersUrl)
      .pipe(tap(orders => this._allOrders.set(orders)));
  }

  /**
   * Cambia el estado de un pedido (solo administrador).
   *
   * @param id     identificador del pedido.
   * @param status nuevo estado.
   * @returns observable con el pedido actualizado.
   */
  public updateOrderStatus(id: number, status: OrderStatus): Observable<Order> {
    return this.http
      .patch<Order>(`${this.ordersUrl}/${id}/status`, { status })
      .pipe(
        tap(updated => this._allOrders.update(list =>
          list.map(order => (order.id === id ? updated : order))
        ))
      );
  }
}
