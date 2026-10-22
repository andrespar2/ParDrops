/**
 * @file src/app/pages/admin-users/admin-users.component.ts
 * @description Sección "Usuarios" del panel de administración.
 *
 * Permite buscar cuentas por texto, filtrarlas por rol y estado, paginar el
 * resultado, consultar la ficha de una cuenta (con su tienda y su actividad) y
 * suspenderla o reactivarla. Consume `GET /api/admin/users`,
 * `GET /api/admin/users/{id}` y `PATCH /api/admin/users/{id}/status`.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AdminService,
  AdminUserFilters
} from '../../core/services/admin.service';
import {
  AdminRole,
  AdminUser,
  AdminUserDetail,
  AdminUserStatus,
  PageResponse
} from '../../core/models/admin.model';
import { formatCurrency, formatDate as formatDateCO } from '../../core/config/commerce.config';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent {
  private readonly adminService = inject(AdminService);

  /** Página de cuentas cargada. */
  public readonly page = signal<PageResponse<AdminUser> | null>(null);

  /** Ficha de la cuenta seleccionada. */
  public readonly detail = signal<AdminUserDetail | null>(null);

  /** Indica si el listado se está cargando. */
  public readonly loading = signal<boolean>(true);

  /** Indica si la ficha se está cargando. */
  public readonly loadingDetail = signal<boolean>(false);

  /** Identificador de la cuenta cuyo estado se está cambiando. */
  public readonly savingId = signal<number | null>(null);

  /** Mensaje de error visible. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de confirmación visible. */
  public readonly okMessage = signal<string | null>(null);

  /** Texto de búsqueda del formulario. */
  public search = '';

  /** Rol seleccionado en el filtro (cadena vacía = todos). */
  public roleFilter: AdminRole | '' = '';

  /** Estado seleccionado en el filtro (cadena vacía = todos). */
  public statusFilter: AdminUserStatus | '' = '';

  /** Página actual (empezando en 0). */
  private currentPage = 0;

  /** Tamaño de página. */
  private readonly pageSize = 10;

  constructor() {
    this.reload();
  }

  /**
   * Recarga el listado desde la primera página.
   *
   * @returns {void}
   */
  public reload(): void {
    this.currentPage = 0;
    this.load();
  }

  /**
   * Carga el listado de cuentas con los filtros actuales.
   *
   * @returns {void}
   */
  public load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const filters: AdminUserFilters = {
      search: this.search,
      role: this.roleFilter || null,
      status: this.statusFilter || null,
      page: this.currentPage,
      size: this.pageSize
    };

    this.adminService.listUsers(filters).subscribe({
      next: page => {
        this.page.set(page);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudieron cargar las cuentas.'
        );
        this.loading.set(false);
      }
    });
  }

  /**
   * Avanza o retrocede de página.
   *
   * @param delta desplazamiento de páginas (-1 o 1).
   * @returns {void}
   */
  public changePage(delta: number): void {
    const total = this.page()?.totalPages ?? 0;
    const next = this.currentPage + delta;
    if (next < 0 || next >= total) return;

    this.currentPage = next;
    this.load();
  }

  /**
   * Carga la ficha completa de una cuenta.
   *
   * @param user cuenta seleccionada.
   * @returns {void}
   */
  public openDetail(user: AdminUser): void {
    this.loadingDetail.set(true);
    this.errorMessage.set(null);
    this.okMessage.set(null);

    this.adminService.getUser(user.id).subscribe({
      next: detail => {
        this.detail.set(detail);
        this.loadingDetail.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cargar la ficha de la cuenta.'
        );
        this.loadingDetail.set(false);
      }
    });
  }

  /** Cierra la ficha abierta. */
  public closeDetail(): void {
    this.detail.set(null);
  }

  /**
   * Suspende o reactiva una cuenta.
   *
   * @param user cuenta a modificar.
   * @returns {void}
   */
  public toggleStatus(user: AdminUser): void {
    const suspend = user.status === 'ACTIVE';
    const action = suspend ? 'suspender' : 'reactivar';
    const confirmed = window.confirm(
      `¿Seguro que quieres ${action} la cuenta de ${user.fullName} (${user.email})?`
    );
    if (!confirmed) return;

    let reason: string | null = null;
    if (suspend) {
      reason = window.prompt('Motivo de la suspensión (opcional):');
    }

    this.savingId.set(user.id);
    this.errorMessage.set(null);
    this.okMessage.set(null);

    const newStatus: AdminUserStatus = suspend ? 'SUSPENDED' : 'ACTIVE';

    this.adminService.changeUserStatus(user.id, newStatus, reason).subscribe({
      next: () => {
        this.savingId.set(null);
        this.okMessage.set(
          `La cuenta de ${user.fullName} se ha ${suspend ? 'suspendido' : 'reactivado'}.`
        );
        this.closeDetail();
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cambiar el estado de la cuenta.'
        );
        this.savingId.set(null);
      }
    });
  }

  /**
   * Devuelve la etiqueta legible de un rol.
   *
   * @param role rol de la cuenta.
   * @returns la etiqueta en español.
   */
  public roleLabel(role: AdminRole): string {
    switch (role) {
      case 'ADMIN': return 'Administrador';
      case 'SELLER': return 'Vendedor';
      default: return 'Cliente';
    }
  }

  /**
   * Devuelve la etiqueta legible de un estado de cuenta.
   *
   * @param status estado de la cuenta.
   * @returns la etiqueta en español.
   */
  public statusLabel(status: AdminUserStatus): string {
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
