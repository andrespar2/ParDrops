/**
 * @file src/app/pages/admin-dashboard/admin-dashboard.component.ts
 * @description Sección "Resumen general" del panel de administración.
 *
 * Muestra los indicadores de la plataforma (cuentas, catálogo, pedidos e
 * ingresos), los últimos pedidos y las tiendas con más ventas. Consume
 * `GET /api/admin/dashboard`.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../core/services/admin.service';
import { AdminDashboard } from '../../core/models/admin.model';
import {
  formatCurrency,
  formatDate as formatDateCO
} from '../../core/config/commerce.config';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent {
  private readonly adminService = inject(AdminService);

  /** Datos del tablero. */
  public readonly dashboard = signal<AdminDashboard | null>(null);

  /** Indica si el resumen está cargándose. */
  public readonly loading = signal<boolean>(true);

  /** Mensaje de error de la carga. */
  public readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.refresh();
  }

  /**
   * Carga (o recarga) el resumen general de la plataforma.
   *
   * @returns {void}
   */
  public refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.adminService.loadDashboard().subscribe({
      next: dashboard => {
        this.dashboard.set(dashboard);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cargar el resumen general.'
        );
        this.loading.set(false);
      }
    });
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
