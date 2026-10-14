/**
 * @file src/app/pages/account/account.component.ts
 * @description Página de la cuenta del usuario.
 *
 * Muestra los datos del usuario autenticado y su historial de pedidos. Solo es
 * accesible con sesión iniciada (protegida por `authGuard`).
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { Order, OrderStatus, orderStatusLabel } from '../../core/models/order.model';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent {
  private readonly authService = inject(AuthService);
  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);

  /** Usuario autenticado. */
  public readonly user = this.authService.user;

  /** Indica si el usuario es administrador. */
  public readonly isAdmin = this.authService.isAdmin;

  /** Pedidos del usuario. */
  public readonly orders = this.orderService.myOrders;

  /** Indica si los pedidos están cargándose. */
  public readonly loading = signal<boolean>(true);

  /** Indica si hubo un error al cargar los pedidos. */
  public readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.orderService.loadMyOrders().subscribe({
      next: () => this.loading.set(false),
      error: () => {
        this.errorMessage.set('No se pudieron cargar tus pedidos.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Cierra la sesión y vuelve al inicio.
   *
   * @returns {void}
   */
  public logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }

  /**
   * Devuelve una etiqueta legible para el estado de un pedido.
   *
   * @param status estado del pedido.
   * @returns etiqueta en español.
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
