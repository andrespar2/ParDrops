/**
 * @file src/app/pages/admin-orders/admin-orders.component.ts
 * @description Panel de administración de pedidos.
 *
 * Lista todos los pedidos realizados en la tienda y permite al administrador
 * consultar los datos de contacto y la dirección de envío de cada cliente, así
 * como avanzar el estado del pedido (preparado, enviado, entregado…). Solo es
 * accesible con rol de administrador (protegida por `adminGuard`).
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { OrderService } from '../../core/services/order.service';
import {
  Order,
  OrderStatus,
  allowedOrderTransitions,
  orderStatusLabel
} from '../../core/models/order.model';

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-orders.component.html',
  styleUrls: ['./admin-orders.component.scss']
})
export class AdminOrdersComponent {
  private readonly orderService = inject(OrderService);

  /** Todos los pedidos (signal del servicio). */
  public readonly orders = this.orderService.allOrders;

  /** Indica si los pedidos están cargándose. */
  public readonly loading = signal<boolean>(true);

  /** Mensaje de error al cargar o actualizar un pedido. */
  public readonly errorMessage = signal<string | null>(null);

  /** Identificador del pedido cuyo estado se está actualizando. */
  public readonly savingId = signal<number | null>(null);

  constructor() {
    this.refresh();
  }

  /**
   * Carga (o recarga) la lista de pedidos.
   *
   * @returns {void}
   */
  public refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.orderService.loadAllOrders().subscribe({
      next: () => this.loading.set(false),
      error: () => {
        this.errorMessage.set('No se pudieron cargar los pedidos.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Cambia el estado de un pedido.
   *
   * @param order  pedido a actualizar.
   * @param status nuevo estado.
   * @returns {void}
   */
  public changeStatus(order: Order, status: OrderStatus): void {
    this.savingId.set(order.id);
    this.errorMessage.set(null);

    this.orderService.updateOrderStatus(order.id, status).subscribe({
      next: () => this.savingId.set(null),
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo actualizar el estado del pedido.'
        );
        this.savingId.set(null);
      }
    });
  }

  /**
   * Estados a los que se puede pasar desde el estado indicado.
   *
   * @param status estado actual del pedido.
   * @returns lista de estados destino permitidos.
   */
  public allowedTransitions(status: OrderStatus): OrderStatus[] {
    return allowedOrderTransitions(status);
  }

  /**
   * Devuelve la etiqueta legible de un estado de pedido.
   *
   * @param status estado del pedido.
   * @returns la etiqueta en español.
   */
  public statusLabel(status: OrderStatus): string {
    return orderStatusLabel(status);
  }

  /**
   * Formatea la fecha de un pedido.
   *
   * @param isoDate fecha en formato ISO.
   * @returns la fecha formateada en español.
   */
  public formatDate(isoDate: string): string {
    return new Date(isoDate).toLocaleDateString('es-ES', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  /**
   * Formatea un importe como moneda.
   *
   * @param price importe a formatear.
   * @returns el importe formateado en euros.
   */
  public formatPrice(price: number): string {
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 2
    }).format(price);
  }

  /**
   * Calcula el número total de artículos de un pedido.
   *
   * @param order pedido.
   * @returns el número total de unidades.
   */
  public orderItemCount(order: Order): number {
    return order.items.reduce((total, item) => total + item.quantity, 0);
  }
}
