/**
 * @file src/app/core/services/order.service.ts
 * @description Servicio para crear y consultar pedidos.
 *
 * Además del alta y la consulta de pedidos, mantiene la lista completa que usa
 * el panel de administración y sondea periódicamente el servidor para avisar al
 * administrador cuando entra una compra nueva.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { CreateOrderRequest, Order, OrderStatus } from '../models/order.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  /** Intervalo (ms) entre comprobaciones de pedidos nuevos. */
  private static readonly NOTIFICATION_POLL_MS = 15000;

  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** URL base de los endpoints de pedidos. */
  private readonly ordersUrl = `${API_BASE_URL}/orders`;

  /** Signal con el historial del usuario autenticado. */
  private readonly _myOrders = signal<Order[]>([]);

  /** Signal con todos los pedidos (vista de administración). */
  private readonly _allOrders = signal<Order[]>([]);

  /** Identificadores de pedidos recibidos que todavía no se han avisado. */
  private readonly _newOrderIds = signal<number[]>([]);

  /** Identificadores de pedidos ya conocidos, para detectar los nuevos. */
  private readonly knownOrderIds = new Set<number>();

  /** Temporizador del sondeo de pedidos nuevos (null si está detenido). */
  private pollHandle: ReturnType<typeof setInterval> | null = null;

  /** Historial de pedidos del usuario (solo lectura). */
  public readonly myOrders = this._myOrders.asReadonly();

  /** Todos los pedidos, para el panel de administración (solo lectura). */
  public readonly allOrders = this._allOrders.asReadonly();

  /** Pedidos recibidos que aún no se han gestionado (estado PENDING). */
  public readonly pendingOrders = computed(() =>
    this._allOrders().filter(order => order.status === 'PENDING')
  );

  /** Número de pedidos pendientes de gestión (contador de avisos). */
  public readonly pendingCount = computed(() => this.pendingOrders().length);

  /** Pedidos nuevos detectados por el sondeo y todavía sin revisar. */
  public readonly newArrivals = computed(() => {
    const ids = new Set(this._newOrderIds());
    return this._allOrders().filter(order => ids.has(order.id));
  });

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
      .pipe(tap(orders => this.applyOrders(orders, false)));
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
        tap(updated => {
          this._allOrders.update(list =>
            list.map(order => (order.id === id ? updated : order))
          );
          // Un pedido ya gestionado deja de contar como aviso pendiente.
          this._newOrderIds.update(ids => ids.filter(orderId => orderId !== id));
        })
      );
  }

  /**
   * Empieza a comprobar periódicamente si han entrado pedidos nuevos.
   *
   * <p>La primera lectura solo registra los pedidos que ya existían, para no
   * avisar de compras antiguas. A partir de ahí, cada sondeo marca como nuevos
   * los pedidos que no se conocían y actualiza la lista del panel. Es
   * idempotente: si el sondeo ya está activo, no hace nada.</p>
   *
   * @returns {void}
   */
  public startNotificationPolling(): void {
    if (typeof window === 'undefined' || this.pollHandle !== null) {
      return;
    }

    this.http.get<Order[]>(this.ordersUrl).subscribe({
      next: orders => this.applyOrders(orders, false),
      error: () => {
        /* Sin avisos: el sondeo no debe afectar al resto de la interfaz. */
      }
    });

    this.pollHandle = setInterval(
      () => this.refreshNotifications(),
      OrderService.NOTIFICATION_POLL_MS
    );
  }

  /**
   * Detiene el sondeo de pedidos nuevos.
   *
   * @returns {void}
   */
  public stopNotificationPolling(): void {
    if (this.pollHandle !== null) {
      clearInterval(this.pollHandle);
      this.pollHandle = null;
    }
  }

  /**
   * Descarta los avisos de pedidos nuevos (el administrador ya los ha visto).
   *
   * @returns {void}
   */
  public acknowledgeNewOrders(): void {
    this._newOrderIds.set([]);
  }

  /**
   * Consulta los pedidos y avisa de los que hayan entrado desde el último sondeo.
   *
   * @returns {void}
   */
  private refreshNotifications(): void {
    this.http.get<Order[]>(this.ordersUrl).subscribe({
      next: orders => this.applyOrders(orders, true),
      error: () => {
        /* Sin avisos: el sondeo no debe afectar al resto de la interfaz. */
      }
    });
  }

  /**
   * Actualiza la lista de pedidos y, si procede, registra los recién llegados.
   *
   * @param orders    pedidos devueltos por la API.
   * @param notifyNew indica si los pedidos desconocidos deben generar un aviso.
   * @returns {void}
   */
  private applyOrders(orders: Order[], notifyNew: boolean): void {
    const arrivals = notifyNew
      ? orders
          .filter(order => !this.knownOrderIds.has(order.id))
          .map(order => order.id)
      : [];

    orders.forEach(order => this.knownOrderIds.add(order.id));
    this._allOrders.set(orders);

    if (arrivals.length > 0) {
      this._newOrderIds.update(ids => Array.from(new Set([...ids, ...arrivals])));
    }
  }
}
