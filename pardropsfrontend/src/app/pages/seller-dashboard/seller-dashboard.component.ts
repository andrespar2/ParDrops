/**
 * @file src/app/pages/seller-dashboard/seller-dashboard.component.ts
 * @description Resumen de actividad del vendedor autenticado.
 *
 * Muestra los siete indicadores de su comercio (ventas, pedidos, pendientes,
 * productos publicados, agotados e ingresos) y sus últimas notificaciones.
 * Las cifras las calcula el backend solo con datos del propio vendedor.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { SellerService } from '../../core/services/seller.service';
import { SellerDashboard } from '../../core/models/seller.model';
import { NotificationType } from '../../core/models/notification.model';
import {
  formatCurrency,
  formatDateTime
} from '../../core/config/commerce.config';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './seller-dashboard.component.html',
  styleUrls: ['./seller-dashboard.component.scss']
})
export class SellerDashboardComponent {
  private readonly sellerService = inject(SellerService);

  /** Datos del resumen. */
  public readonly dashboard = signal<SellerDashboard | null>(null);

  /** Indica si el resumen está cargándose. */
  public readonly loading = signal<boolean>(true);

  /** Mensaje de error de la carga. */
  public readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.refresh();
  }

  /**
   * Carga (o recarga) el resumen del vendedor.
   *
   * @returns {void}
   */
  public refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.sellerService.getDashboard().subscribe({
      next: dashboard => {
        this.dashboard.set(dashboard);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cargar el resumen de tu tienda.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Formatea un importe en pesos colombianos.
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
   * Devuelve una etiqueta legible para el tipo de aviso.
   *
   * @param type tipo de notificación.
   * @returns la etiqueta en español.
   */
  public notificationLabel(type: NotificationType): string {
    const labels: Record<NotificationType, string> = {
      INFO: 'Información',
      SELLER_APPLICATION: 'Solicitud',
      ORDER: 'Pedido',
      PRODUCT: 'Producto',
      ACCOUNT: 'Cuenta'
    };
    return labels[type] ?? type;
  }
}
