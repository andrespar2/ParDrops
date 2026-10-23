/**
 * @file src/app/pages/admin-applications/admin-applications.component.ts
 * @description Sección "Solicitudes" del panel de administración.
 *
 * Lista las solicitudes para convertirse en vendedor y permite aprobarlas o
 * rechazarlas con un motivo. Consume `GET /api/seller-applications` y
 * `PATCH /api/seller-applications/{id}/decision`.
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
  SellerApplication,
  SellerApplicationStatus
} from '../../core/models/admin.model';
import { formatDate as formatDateCO } from '../../core/config/commerce.config';

@Component({
  selector: 'app-admin-applications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-applications.component.html',
  styleUrls: ['./admin-applications.component.scss']
})
export class AdminApplicationsComponent {
  private readonly adminService = inject(AdminService);

  /** Listado de solicitudes. */
  public readonly applications = signal<SellerApplication[]>([]);

  /** Indica si el listado está cargándose. */
  public readonly loading = signal<boolean>(true);

  /** Identificador de la solicitud que se está resolviendo. */
  public readonly savingId = signal<number | null>(null);

  /** Mensaje de error visible. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de confirmación visible. */
  public readonly okMessage = signal<string | null>(null);

  /** Estado seleccionado en el filtro. */
  public statusFilter: SellerApplicationStatus | '' = '';

  constructor() {
    this.load();
  }

  /**
   * Carga el listado de solicitudes con el filtro actual.
   *
   * @returns {void}
   */
  public load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.adminService.listApplications(this.statusFilter || null).subscribe({
      next: applications => {
        this.applications.set(applications);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudieron cargar las solicitudes.'
        );
        this.loading.set(false);
      }
    });
  }

  /**
   * Aprueba una solicitud.
   *
   * @param application solicitud a aprobar.
   * @returns {void}
   */
  public approve(application: SellerApplication): void {
    const confirmed = window.confirm(
      `¿Aprobar la solicitud de ${application.applicantName} ` +
      `(${application.businessName})? Se creará su tienda.`
    );
    if (!confirmed) return;

    this.decide(application.id, true, null);
  }

  /**
   * Rechaza una solicitud pidiendo el motivo.
   *
   * @param application solicitud a rechazar.
   * @returns {void}
   */
  public reject(application: SellerApplication): void {
    const reason = window.prompt(
      'Motivo del rechazo (obligatorio):'
    );
    if (reason === null) return;

    if (!reason.trim()) {
      this.errorMessage.set('Debes indicar un motivo para rechazar la solicitud.');
      return;
    }

    this.decide(application.id, false, reason.trim());
  }

  /**
   * Envía la decisión a la API.
   *
   * @param id      identificador de la solicitud.
   * @param approve `true` para aprobar, `false` para rechazar.
   * @param notes   motivo de la decisión.
   * @returns {void}
   * @private
   */
  private decide(id: number, approve: boolean, notes: string | null): void {
    this.savingId.set(id);
    this.errorMessage.set(null);
    this.okMessage.set(null);

    this.adminService
      .decideApplication(id, { approve, reviewNotes: notes })
      .subscribe({
        next: () => {
          this.savingId.set(null);
          this.okMessage.set(approve ? 'Solicitud aprobada.' : 'Solicitud rechazada.');
          this.load();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(
            error.error?.message || 'No se pudo resolver la solicitud.'
          );
          this.savingId.set(null);
        }
      });
  }

  /**
   * Devuelve la etiqueta legible del estado de una solicitud.
   *
   * @param status estado de la solicitud.
   * @returns la etiqueta en español.
   */
  public statusLabel(status: SellerApplicationStatus): string {
    switch (status) {
      case 'APPROVED': return 'Aprobada';
      case 'REJECTED': return 'Rechazada';
      default: return 'Pendiente';
    }
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
}
