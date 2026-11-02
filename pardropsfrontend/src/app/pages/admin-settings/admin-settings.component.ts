/**
 * @file src/app/pages/admin-settings/admin-settings.component.ts
 * @description SecciÃ³n "ConfiguraciÃ³n" del panel de administraciÃ³n.
 *
 * Muestra la configuraciÃ³n general de la plataforma tal y como la aplica el
 * frontend (moneda, impuesto, envÃ­o y umbral de envÃ­o gratuito) y el estado del
 * catÃ¡logo y de las categorÃ­as, leÃ­do de la API real. La configuraciÃ³n de
 * negocio es de solo lectura: vive en el cÃ³digo y en la base de datos, no se
 * edita desde aquÃ­ para no falsear cobros ni envÃ­os.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminService } from '../../core/services/admin.service';
import { AdminDashboard } from '../../core/models/admin.model';
import {
  CURRENCY_CODE,
  DEFAULT_COUNTRY,
  FREE_SHIPPING_THRESHOLD,
  SHIPPING_COST,
  INCLUDED_TAX_RATE,
  formatCurrency
} from '../../core/config/commerce.config';

@Component({
  selector: 'app-admin-settings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-settings.component.html',
  styleUrls: ['./admin-settings.component.scss']
})
export class AdminSettingsComponent {
  private readonly adminService = inject(AdminService);

  /** CÃ³digo de moneda de la plataforma. */
  public readonly currencyCode = CURRENCY_CODE;

  /** PaÃ­s de la tienda. */
  public readonly country = DEFAULT_COUNTRY;

  /** Porcentaje de IVA aplicado. */
  public readonly taxRate = INCLUDED_TAX_RATE;

  /** Coste de envÃ­o. */
  public readonly shippingCost = SHIPPING_COST;

  /** Umbral de envÃ­o gratuito. */
  public readonly freeShippingThreshold = FREE_SHIPPING_THRESHOLD;

  /** Resumen de la plataforma, usado para mostrar el estado del catÃ¡logo. */
  public readonly dashboard = signal<AdminDashboard | null>(null);

  /** Indica si el resumen estÃ¡ cargÃ¡ndose. */
  public readonly loading = signal<boolean>(true);

  /** Mensaje de error visible. */
  public readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.load();
  }

  /**
   * Carga el estado actual del catÃ¡logo y las categorÃ­as.
   *
   * @returns {void}
   */
  public load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.adminService.loadDashboard().subscribe({
      next: dashboard => {
        this.dashboard.set(dashboard);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cargar el estado de la plataforma.'
        );
        this.loading.set(false);
      }
    });
  }

  /**
   * Formatea un porcentaje como texto legible.
   *
   * @param rate tasa expresada en tanto por uno.
   * @returns el porcentaje formateado.
   */
  public formatRate(rate: number): string {
    return `${Math.round(rate * 100)}%`;
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

