/**
 * @file src/app/pages/seller-application/seller-application.component.ts
 * @description Solicitud de un cliente para convertirse en vendedor.
 *
 * Consulta si el usuario ya tiene una solicitud y muestra su estado. Si no
 * tiene ninguna, permite enviarla. El administrador la revisa desde el panel;
 * al aprobarla se crea la tienda y la cuenta pasa a rol SELLER.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SellerService } from '../../core/services/seller.service';
import {
  SellerApplication,
  SellerApplicationRequest
} from '../../core/models/seller.model';
import { formatDateTime } from '../../core/config/commerce.config';

@Component({
  selector: 'app-seller-application',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './seller-application.component.html',
  styleUrls: ['./seller-application.component.scss']
})
export class SellerApplicationComponent {
  private readonly sellerService = inject(SellerService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  /** Indica si el usuario ya es vendedor. */
  public readonly isSeller = this.authService.isSeller;

  /** Solicitud existente (si la hay). */
  public readonly application = signal<SellerApplication | null>(null);

  /** Indica si se está consultando la solicitud. */
  public readonly loading = signal<boolean>(true);

  /** Indica si se está enviando. */
  public readonly sending = signal<boolean>(false);

  /** Indica si se intentó enviar. */
  public readonly submitted = signal<boolean>(false);

  /** Mensaje de error. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de éxito. */
  public readonly successMessage = signal<string | null>(null);

  /** Formulario de solicitud. */
  public readonly form = this.fb.nonNullable.group({
    businessName: ['', [Validators.required, Validators.maxLength(120)]],
    taxId: ['', [Validators.maxLength(40)]],
    description: ['', [Validators.maxLength(2000)]],
    phone: ['', [Validators.maxLength(30)]],
    city: ['', [Validators.maxLength(120)]],
    publicEmail: ['', [Validators.email, Validators.maxLength(180)]],
    publicPhone: ['', [Validators.maxLength(30)]],
    legalName: ['', [Validators.maxLength(120)]],
    documentType: ['', [Validators.maxLength(20)]],
    documentNumber: ['', [Validators.maxLength(40)]],
    legalAddress: ['', [Validators.maxLength(300)]],
    personalPhone: ['', [Validators.maxLength(30)]]
  });

  constructor() {
    if (this.isSeller()) {
      this.loading.set(false);
    } else {
      this.loadApplication();
    }
  }

  /**
   * Consulta la solicitud del usuario autenticado.
   *
   * @returns {void}
   */
  public loadApplication(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.sellerService.getMyApplication().subscribe({
      next: application => {
        this.application.set(application);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status !== 404) {
          this.errorMessage.set(
            error.error?.message || 'No se pudo consultar tu solicitud.');
        }
        this.application.set(null);
        this.loading.set(false);
      }
    });
  }

  /**
   * Envía la solicitud para vender.
   *
   * @returns {void}
   */
  public send(): void {
    this.submitted.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request: SellerApplicationRequest = {
      businessName: value.businessName.trim(),
      taxId: value.taxId.trim() || null,
      description: value.description.trim() || null,
      phone: value.phone.trim() || null,
      city: value.city.trim() || null,
      publicEmail: value.publicEmail.trim() || null,
      publicPhone: value.publicPhone.trim() || null,
      legalName: value.legalName.trim() || null,
      documentType: value.documentType.trim() || null,
      documentNumber: value.documentNumber.trim() || null,
      legalAddress: value.legalAddress.trim() || null,
      personalPhone: value.personalPhone.trim() || null
    };

    this.sending.set(true);
    this.sellerService.submitApplication(request).subscribe({
      next: application => {
        this.application.set(application);
        this.sending.set(false);
        this.successMessage.set(
          'Solicitud enviada. El equipo de ParDrops la revisará pronto.');
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo enviar la solicitud.');
        this.sending.set(false);
      }
    });
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
   * Etiqueta legible del estado de la solicitud.
   *
   * @param status estado.
   * @returns la etiqueta en español.
   */
  public statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En revisión',
      APPROVED: 'Aprobada',
      REJECTED: 'Rechazada'
    };
    return labels[status] ?? status;
  }
}
