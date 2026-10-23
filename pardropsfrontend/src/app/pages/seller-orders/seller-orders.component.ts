/**
 * @file src/app/pages/seller-orders/seller-orders.component.ts
 * @description Preparación de los pedidos que corresponden al vendedor.
 *
 * Solo se muestran las partes de pedido (subpedidos) de la tienda
 * autenticada. El vendedor puede avanzar su estado siguiendo las transiciones
 * que valida el backend (preparado, enviado, entregado o cancelado).
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { SellerService } from '../../core/services/seller.service';
import { SellerOrder } from '../../core/models/seller.model';
import {
  OrderStatus,
  allowedOrderTransitions,
  orderStatusLabel
} from '../../core/models/order.model';
import { formatCurrency, formatDateTime } from '../../core/config/commerce.config';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seller-orders.component.html',
  styleUrls: ['./seller-orders.component.scss']
})
export class SellerOrdersComponent {
  private readonly sellerService = inject(SellerService);

  /** Subpedidos del vendedor. */
  public readonly orders = signal<SellerOrder[]>([]);

  /** Indica si se están cargando. */
  public readonly loading = signal<boolean>(true);

  /** Indica si hay un cambio de estado en curso. */
  public readonly updating = signal<number | null>(null);

  /** Filtro por estado (cadena vacía para todos). */
  public readonly statusFilter = signal<OrderStatus | ''>('');

  /** Mensaje de error. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de éxito. */
  public readonly successMessage = signal<string | null>(null);

  /** Estados disponibles para el filtro. */
  public readonly statuses: OrderStatus[] = [
    'PENDING', 'PAID', 'PREPARED', 'SHIPPED', 'DELIVERED', 'CANCELLED'
  ];

  /** Pedidos visibles según el filtro. */
  public readonly filteredOrders = computed(() => {
    const filter = this.statusFilter();
    return filter
      ? this.orders().filter(order => order.status === filter)
      : this.orders();
  });

  constructor() {
    this.loadOrders();
  }

  /**
   * Carga los subpedidos del vendedor.
   *
   * @returns {void}
   */
  public loadOrders(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.sellerService.listOrders().subscribe({
      next: orders => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudieron cargar tus pedidos.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Cambia el filtro de estado.
   *
   * @param value estado elegido o cadena vacía.
   * @returns {void}
   */
  public onFilterChange(value: string): void {
    this.statusFilter.set(value as OrderStatus | '');
  }

  /**
   * Devuelve los estados a los que puede pasar un subpedido.
   *
   * @param order subpedido actual.
   * @returns los estados permitidos.
   */
  public transitions(order: SellerOrder): OrderStatus[] {
    return allowedOrderTransitions(order.status);
  }

  /**
   * Avanza el estado de un subpedido propio.
   *
   * @param order  subpedido.
   * @param status nuevo estado.
   * @returns {void}
   */
  public changeStatus(order: SellerOrder, status: OrderStatus): void {
    this.updating.set(order.id);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.sellerService.updateOrderStatus(order.id, status).subscribe({
      next: updated => {
        this.orders.update(list =>
          list.map(item => (item.id === updated.id ? updated : item)));
        this.successMessage.set(
          `Pedido #${updated.orderId}: ${orderStatusLabel(updated.status)}.`);
        this.updating.set(null);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo actualizar el estado del pedido.');
        this.updating.set(null);
      }
    });
  }

  /**
   * Etiqueta legible de un estado.
   *
   * @param status estado.
   * @returns la etiqueta en español.
   */
  public statusLabel(status: OrderStatus): string {
    return orderStatusLabel(status);
  }

  /**
   * Formatea un importe.
   *
   * @param value importe.
   * @returns el importe formateado.
   */
  public formatPrice(value: number): string {
    return formatCurrency(value);
  }

  /**
   * Formatea una fecha con hora.
   *
   * @param value fecha ISO.
   * @returns la fecha legible.
   */
  public formatDate(value: string): string {
    return formatDateTime(value);
  }

  /**
   * Indica si un subpedido ya es final.
   *
   * @param order subpedido.
   * @returns {@code true} si está entregado o cancelado.
   */
  public isFinal(order: SellerOrder): boolean {
    return order.status === 'DELIVERED' || order.status === 'CANCELLED';
  }
}
