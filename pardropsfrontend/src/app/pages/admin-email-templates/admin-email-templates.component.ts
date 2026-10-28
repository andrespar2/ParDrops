/**
 * @file src/app/pages/admin-email-templates/admin-email-templates.component.ts
 * @description Editor de las plantillas de correo transaccional.
 *
 * Permite elegir una plantilla, editar el asunto y el cuerpo HTML, activarla o
 * desactivarla, previsualizarla con datos de ejemplo y restaurar los valores
 * predeterminados.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { AdminService } from '../../core/services/admin.service';
import { EmailTemplate, EmailTemplateCode } from '../../core/models/admin.model';

@Component({
  selector: 'app-admin-email-templates',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-email-templates.component.html',
  styleUrls: ['./admin-email-templates.component.scss']
})
export class AdminEmailTemplatesComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly fb = inject(FormBuilder);
  private readonly sanitizer = inject(DomSanitizer);

  /** Plantillas disponibles. */
  public readonly templates = signal<EmailTemplate[]>([]);

  /** Código de la plantilla seleccionada. */
  public readonly selectedCode = signal<EmailTemplateCode>('SELLER_NEW_ORDER');

  /** Indica si se están cargando las plantillas. */
  public readonly loading = signal<boolean>(true);

  /** Indica si se está guardando. */
  public readonly saving = signal<boolean>(false);

  /** Indica si se está generando la vista previa. */
  public readonly previewing = signal<boolean>(false);

  /** Mensaje de error. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de éxito. */
  public readonly okMessage = signal<string | null>(null);

  /** Asunto de la vista previa. */
  public readonly previewSubject = signal<string>('');

  /** HTML de la vista previa, listo para el iframe. */
  public readonly preview = signal<SafeHtml | null>(null);

  /** Variables que se pueden usar en las plantillas. */
  public readonly variables = [
    'orderId',
    'orderDate',
    'customerName',
    'customerEmail',
    'customerPhone',
    'address',
    'storeName',
    'sellerName',
    'itemsTable',
    'subtotal',
    'tax',
    'shipping',
    'total',
    'status',
    'statusLabel',
    'accountUrl',
    'sellerOrdersUrl'
  ];

  /** Formulario de edición. */
  public readonly form = this.fb.nonNullable.group({
    subject: ['', [Validators.required, Validators.maxLength(250)]],
    bodyHtml: ['', [Validators.required]],
    active: [true]
  });

  /**
   * Carga las plantillas al abrir la página.
   *
   * @returns {void}
   */
  public ngOnInit(): void {
    this.load();
  }

  /**
   * Selecciona una plantilla para editarla.
   *
   * @param template plantilla seleccionada.
   * @returns {void}
   */
  public select(template: EmailTemplate): void {
    this.selectedCode.set(template.code);
    this.form.patchValue({
      subject: template.subject,
      bodyHtml: template.bodyHtml,
      active: template.active
    });
    this.preview.set(null);
    this.previewSubject.set('');
    this.errorMessage.set(null);
    this.okMessage.set(null);
  }

  /**
   * Guarda los cambios de la plantilla seleccionada.
   *
   * @returns {void}
   */
  public save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    this.okMessage.set(null);

    this.adminService.updateEmailTemplate(this.selectedCode(), this.form.getRawValue()).subscribe({
      next: updated => {
        this.templates.update(list =>
          list.map(template => (template.code === updated.code ? updated : template))
        );
        this.okMessage.set('Plantilla guardada. Se aplicará al próximo correo.');
        this.saving.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.message(error));
        this.saving.set(false);
      }
    });
  }

  /**
   * Restaura la plantilla predeterminada.
   *
   * @returns {void}
   */
  public reset(): void {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.okMessage.set(null);

    this.adminService.resetEmailTemplate(this.selectedCode()).subscribe({
      next: updated => {
        this.templates.update(list =>
          list.map(template => (template.code === updated.code ? updated : template))
        );
        this.select(updated);
        this.okMessage.set('Plantilla restaurada a los valores predeterminados.');
        this.saving.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.message(error));
        this.saving.set(false);
      }
    });
  }

  /**
   * Genera la vista previa con datos de ejemplo.
   *
   * @returns {void}
   */
  public showPreview(): void {
    this.previewing.set(true);
    this.errorMessage.set(null);

    this.adminService.previewEmailTemplate(this.selectedCode()).subscribe({
      next: rendered => {
        this.previewSubject.set(rendered.subject);
        this.preview.set(this.sanitizer.bypassSecurityTrustHtml(rendered.bodyHtml));
        this.previewing.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.message(error));
        this.previewing.set(false);
      }
    });
  }

  /**
   * Devuelve la plantilla seleccionada.
   *
   * @returns la plantilla o `undefined`.
   */
  public selectedTemplate(): EmailTemplate | undefined {
    return this.templates().find(template => template.code === this.selectedCode());
  }

  /** Carga la lista de plantillas. */
  private load(): void {
    this.loading.set(true);
    this.adminService.listEmailTemplates().subscribe({
      next: list => {
        this.templates.set(list);
        const first = list.find(template => template.code === this.selectedCode()) ?? list[0];
        if (first) {
          this.select(first);
        }
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.message(error));
        this.loading.set(false);
      }
    });
  }

  /** Extrae un mensaje legible del error. */
  private message(error: HttpErrorResponse): string {
    return error.error?.message || 'No se pudo completar la operación. Inténtalo de nuevo.';
  }
}
