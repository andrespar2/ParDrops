/**
 * @file src/app/pages/admin-sellers/admin-sellers.component.ts
 * @description Sección "Vendedores" del panel de administración.
 *
 * Lista los vendedores con su tienda y su actividad, permite filtrar por estado
 * de la tienda y por texto, y suspender o reactivar la cuenta del vendedor.
 * Consume `GET /api/admin/sellers` y `PATCH /api/admin/users/{id}/status`.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminService } from '../../core/services/admin.service';
import {
  AdminSeller,
  AdminStoreStatus,
  AdminUserStatus
} from '../../core/models/admin.model';
import { formatCurrency, formatDate as formatDateCO } from '../../core/config/commerce.config';

@Component({
  selector: 'app-admin-sellers',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-sellers.component.html',
  styleUrls: ['./admin-sellers.component.scss']
})
export class AdminSellersComponent {
  private readonly adminService = inject(AdminService);

  /** Listado de vendedores. */
  public readonly sellers = signal<AdminSeller[]>([]);

  /** Indica si el listado está cargándose. */
  public readonly loading = signal<boolean>(true);

  /** Identificador del vendedor cuyo estado se está cambiando. */
  public readonly savingId = signal<number | null>(null);

  /** Mensaje de error visible. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de confirmación visible. */
  public readonly okMessage = signal<string | null>(null);

  /** Texto de búsqueda del formulario. */
  public search = '';

  /** Estado de tienda seleccionado en el filtro. */
  public statusFilter: AdminStoreStatus | '' = '';

  constructor() {
    this.load();
  }

  /**
   * Carga el listado de vendedores con los filtros actuales.
   *
   * @returns {void}
   */
  public load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.adminService
      .listSellers(this.statusFilter || null, this.search)
      .subscribe({
        next: sellers => {
          this.sellers.set(sellers);
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(
            error.error?.message || 'No se pudieron cargar los vendedores.'
          );
          this.loading.set(false);
        }
      });
  }

  /**
   * Suspende o reactiva la cuenta de un vendedor.
   *
   * @param seller vendedor a modificar.
   * @returns {void}
   */
  public toggleStatus(seller: AdminSeller): void {
    const suspend = seller.accountStatus === 'ACTIVE';
    const action = suspend ? 'suspender' : 'reactivar';
    const confirmed = window.confirm(
      `¿Seguro que quieres ${action} la cuenta de ${seller.fullName} ` +
      `(${seller.storeName})?`
    );
    if (!confirmed) return;

    let reason: string | null = null;
    if (suspend) {
      reason = window.prompt('Motivo de la suspensión (opcional):');
    }

    const newStatus: AdminUserStatus = suspend ? 'SUSPENDED' : 'ACTIVE';

    this.savingId.set(seller.sellerId);
    this.errorMessage.set(null);
    this.okMessage.set(null);

    this.adminService
      .changeUserStatus(seller.sellerId, newStatus, reason)
      .subscribe({
        next: () => {
          this.savingId.set(null);
          this.okMessage.set(
            `La cuenta de ${seller.fullName} se ha ${suspend ? 'suspendido' : 'reactivado'}.`
          );
          this.load();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(
            error.error?.message || 'No se pudo cambiar el estado del vendedor.'
          );
          this.savingId.set(null);
        }
      });
  }

  /**
   * Devuelve la etiqueta legible del estado de una tienda.
   *
   * @param status estado de la tienda.
   * @returns la etiqueta en español.
   */
  public storeStatusLabel(status: AdminStoreStatus): string {
    switch (status) {
      case 'ACTIVE': return 'Activa';
      case 'SUSPENDED': return 'Suspendida';
      default: return 'Pendiente';
    }
  }

  /**
   * Devuelve la etiqueta legible del estado de una cuenta.
   *
   * @param status estado de la cuenta.
   * @returns la etiqueta en español.
   */
  public accountStatusLabel(status: AdminUserStatus): string {
    return status === 'ACTIVE' ? 'Activa' : 'Suspendida';
  }

  /**
   * Formatea una fecha en formato ISO.
   *
   * @param isoDate fecha de origen.
   * @returns la fecha formateada en español.
   */
  public formatDate(isoDate: string): string {
    return formatDateCO(isoDate);
  }

  /**
   * Formatea un importe como moneda.
   *
   * @param amount importe a formatear.
   * @returns el importe formateado.
   */
  public formatPrice(amount: number): string {
    return formatCurrency(amount);
  }
}
