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

  /** Tiendas seleccionadas para acciones en masa. */
  public readonly selected = signal<Set<number>>(new Set<number>());

  /** Indica si una acción en masa está en curso. */
  public readonly bulkSaving = signal<boolean>(false);

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
          this.selected.set(new Set());
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
  public toggleStatus(seller: AdminSeller): void {    const suspend = seller.accountStatus === 'ACTIVE';
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
   * Añade o quita una tienda de la selección.
   *
   * @param storeId identificador de la tienda.
   */
  public toggleSelection(storeId: number): void {
    this.selected.update(current => {
      const next = new Set(current);
      if (next.has(storeId)) next.delete(storeId); else next.add(storeId);
      return next;
    });
  }

  /** Indica si una tienda está seleccionada. */
  public isSelected(storeId: number): boolean {
    return this.selected().has(storeId);
  }

  /** Indica si todas las tiendas visibles están seleccionadas. */
  public allSelected(): boolean {
    const sellers = this.sellers();
    return sellers.length > 0 && sellers.every(seller => this.selected().has(seller.storeId));
  }

  /** Limpia la selección. */
  public clearSelection(): void {
    this.selected.set(new Set());
  }

  /** Selecciona o deselecciona todas las tiendas visibles. */
  public toggleAll(): void {
    if (this.allSelected()) {
      this.selected.set(new Set());
      return;
    }
    this.selected.set(new Set(this.sellers().map(seller => seller.storeId)));
  }

  /**
   * Elimina una tienda con confirmación.
   *
   * @param seller tienda/vendedor a eliminar.
   */
  public deleteStore(seller: AdminSeller): void {
    const confirmed = window.confirm(
      `¿Eliminar la tienda "${seller.storeName}"?\n\n` +
      'Dejará de mostrarse en el marketplace. Los pedidos y el historial se conservan.'
    );
    if (!confirmed) return;

    this.bulkSaving.set(true);
    this.errorMessage.set(null);
    this.okMessage.set(null);
    this.adminService.deleteStore(seller.storeId).subscribe({
      next: result => {
        this.bulkSaving.set(false);
        this.okMessage.set(
          `Tienda eliminada. ${result.updated} cambio(s) aplicado(s).`
        );
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.bulkSaving.set(false);
        this.errorMessage.set(
          error.error?.message || 'No se pudo eliminar la tienda.'
        );
      }
    });
  }

  /** Elimina en masa las tiendas seleccionadas. */
  public bulkDelete(): void {
    const ids = [...this.selected()];
    if (ids.length === 0) return;
    const confirmed = window.confirm(
      `¿Eliminar ${ids.length} tienda(s) seleccionada(s)?\n\n` +
      'Dejarán de mostrarse en el marketplace. Los pedidos y el historial se conservan.'
    );
    if (!confirmed) return;

    this.bulkSaving.set(true);
    this.errorMessage.set(null);
    this.okMessage.set(null);
    this.adminService.bulkDeleteStores(ids).subscribe({
      next: result => {
        this.bulkSaving.set(false);
        this.okMessage.set(
          `${result.updated} tienda(s) eliminada(s); ${result.skipped} ya estaban eliminadas.`
        );
        this.selected.set(new Set());
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.bulkSaving.set(false);
        this.errorMessage.set(
          error.error?.message || 'No se pudieron eliminar las tiendas seleccionadas.'
        );
      }
    });
  }

  /**
   * Restaura una tienda eliminada.
   *
   * @param seller tienda/vendedor a restaurar.
   */
  public restoreStore(seller: AdminSeller): void {
    const confirmed = window.confirm(
      `¿Restaurar la tienda "${seller.storeName}"? Volverá a estar disponible según el estado de la cuenta.`
    );
    if (!confirmed) return;

    this.bulkSaving.set(true);
    this.errorMessage.set(null);
    this.okMessage.set(null);
    this.adminService.restoreStore(seller.storeId).subscribe({
      next: () => {
        this.bulkSaving.set(false);
        this.okMessage.set('Tienda restaurada.');
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.bulkSaving.set(false);
        this.errorMessage.set(
          error.error?.message || 'No se pudo restaurar la tienda.'
        );
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
      case 'DELETED': return 'Eliminada';
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
