/**
 * @file src/app/pages/admin-payments/admin-payments.component.ts
 * @description Panel de transacciones de Wompi.
 *
 * Muestra el estado de cada pago, el desglose cobrado, la comisión de
 * ParDrops, la distribución por vendedor y el total recaudado por el cargo
 * fijo de servicio. Solo es accesible con rol ADMIN.
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../core/services/admin.service';
import {
  AdminPaymentTransaction,
  AdminPayments
} from '../../core/models/checkout.model';
import { formatCurrency, formatDateTime } from '../../core/config/commerce.config';

/** Filtros disponibles sobre las transacciones. */
type PaymentFilter = 'ALL' | 'APPROVED' | 'PENDING' | 'REJECTED';

@Component({
  selector: 'app-admin-payments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-payments.component.html',
  styleUrls: ['./admin-payments.component.scss']
})
export class AdminPaymentsComponent {
  private readonly adminService = inject(AdminService);

  /** Resumen recibido de la API. */
  public readonly summary = signal<AdminPayments | null>(null);

  /** Indica si la carga está en curso. */
  public readonly loading = signal<boolean>(true);

  /** Error de carga. */
  public readonly errorMessage = signal<string | null>(null);

  /** Filtro activo. */
  public readonly filter = signal<PaymentFilter>('ALL');

  /** Transacciones filtradas. */
  public readonly transactions = computed(() => {
    const data = this.summary();
    if (!data) {
      return [];
    }
    const current = this.filter();
    return data.transactions.filter(transaction => {
      if (current === 'ALL') {
        return true;
      }
      if (current === 'APPROVED') {
        return transaction.paymentStatus === 'APPROVED';
      }
      if (current === 'PENDING') {
        return !transaction.paymentStatus || transaction.paymentStatus === 'PENDING';
      }
      return transaction.paymentStatus === 'DECLINED'
        || transaction.paymentStatus === 'VOIDED'
        || transaction.paymentStatus === 'ERROR';
    });
  });

  constructor() {
    this.refresh();
  }

  /**
   * Carga las transacciones desde el backend.
   *
   * @returns {void}
   */
  public refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.adminService.loadPayments().subscribe({
      next: data => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('No se pudieron cargar las transacciones de Wompi.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Cambia el filtro activo.
   *
   * @param filter filtro seleccionado.
   * @returns {void}
   */
  public setFilter(filter: PaymentFilter): void {
    this.filter.set(filter);
  }

  /**
   * Formatea un importe como moneda.
   *
   * @param value importe.
   * @returns el importe en pesos colombianos.
   */
  public formatPrice(value: number | null | undefined): string {
    return formatCurrency(value ?? 0);
  }

  /**
   * Formatea una fecha con hora.
   *
   * @param value fecha ISO.
   * @returns la fecha en español de Colombia.
   */
  public formatDate(value: string | null | undefined): string {
    return value ? formatDateTime(value) : '—';
  }

  /**
   * Etiqueta legible del estado del pago.
   *
   * @param status estado del pago.
   * @returns la etiqueta en español.
   */
  public paymentStatusLabel(status: string | null | undefined): string {
    return switchStatus(status);
  }

  /**
   * Clase CSS según el estado del pago.
   *
   * @param status estado del pago.
   * @returns la clase del indicador.
   */
  public statusClass(status: string | null | undefined): string {
    switch (status) {
      case 'APPROVED':
        return 'status--approved';
      case 'DECLINED':
      case 'VOIDED':
      case 'ERROR':
        return 'status--rejected';
      default:
        return 'status--pending';
    }
  }

  /**
   * Indica si la transacción tiene pago aprobado.
   *
   * @param transaction transacción.
   * @returns `true` si está aprobada.
   */
  public isApproved(transaction: AdminPaymentTransaction): boolean {
    return transaction.paymentStatus === 'APPROVED';
  }
}

/**
 * Traduce el estado de Wompi al español.
 *
 * @param status estado recibido.
 * @returns la etiqueta legible.
 */
function switchStatus(status: string | null | undefined): string {
  switch (status) {
    case 'APPROVED':
      return 'Aprobado';
    case 'PENDING':
      return 'Pendiente';
    case 'DECLINED':
      return 'Rechazado';
    case 'VOIDED':
      return 'Anulado';
    case 'ERROR':
      return 'Error';
    default:
      return 'Sin pago';
  }
}
