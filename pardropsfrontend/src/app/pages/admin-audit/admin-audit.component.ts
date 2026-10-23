/**
 * @file src/app/pages/admin-audit/admin-audit.component.ts
 * @description Sección "Auditoría" del panel de administración.
 *
 * Muestra los últimos registros de la bitácora de auditoría (quién hizo qué y
 * cuándo). Consume `GET /api/admin/audit-logs`.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminService } from '../../core/services/admin.service';
import { AuditLog } from '../../core/models/admin.model';
import { formatDateTime as formatDateTimeCO } from '../../core/config/commerce.config';

@Component({
  selector: 'app-admin-audit',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-audit.component.html',
  styleUrls: ['./admin-audit.component.scss']
})
export class AdminAuditComponent {
  private readonly adminService = inject(AdminService);

  /** Registros de auditoría. */
  public readonly logs = signal<AuditLog[]>([]);

  /** Indica si el listado está cargándose. */
  public readonly loading = signal<boolean>(true);

  /** Mensaje de error visible. */
  public readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.load();
  }

  /**
   * Carga (o recarga) la bitácora de auditoría.
   *
   * @returns {void}
   */
  public load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.adminService.listAuditLogs().subscribe({
      next: logs => {
        this.logs.set(logs);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cargar la auditoría.'
        );
        this.loading.set(false);
      }
    });
  }

  /**
   * Devuelve una etiqueta legible para el tipo de acción.
   *
   * @param action tipo de acción registrado.
   * @returns la etiqueta en español.
   */
  public actionLabel(action: string): string {
    const labels: Record<string, string> = {
      USER_SUSPENDED: 'Cuenta suspendida',
      USER_REACTIVATED: 'Cuenta reactivada',
      PRODUCT_HIDDEN: 'Producto oculto',
      PRODUCT_PUBLISHED: 'Producto publicado',
      PRODUCT_DELETED: 'Producto eliminado',
      CATEGORY_CREATED: 'Categoría creada',
      CATEGORY_UPDATED: 'Categoría actualizada',
      CATEGORY_DELETED: 'Categoría eliminada',
      APPLICATION_APPROVED: 'Solicitud aprobada',
      APPLICATION_REJECTED: 'Solicitud rechazada'
    };
    return labels[action] ?? action;
  }

  /**
   * Formatea una fecha y hora en formato ISO.
   *
   * @param isoDate fecha de origen.
   * @returns la fecha y hora formateadas en español.
   */
  public formatDateTime(isoDate: string): string {
    return formatDateTimeCO(isoDate);
  }
}
