/**
 * @file src/app/pages/admin-email-templates/admin-email-templates.component.spec.ts
 * @description Pruebas del editor de plantillas de correo.
 */

import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminEmailTemplatesComponent } from './admin-email-templates.component';
import { AdminService } from '../../core/services/admin.service';
import { EmailTemplate } from '../../core/models/admin.model';

const TEMPLATES: EmailTemplate[] = [
  {
    id: 1,
    code: 'SELLER_NEW_ORDER',
    name: 'Nuevo pedido para el vendedor',
    description: 'Se envía al vendedor',
    subject: 'Nuevo pedido #{{orderId}}',
    bodyHtml: '<p>Hola {{sellerName}}</p>',
    active: true,
    updatedAt: '2026-10-28T10:00:00Z'
  },
  {
    id: 2,
    code: 'BUYER_PAYMENT_RECEIPT',
    name: 'Comprobante de compra',
    description: 'Se envía al comprador al pagar',
    subject: 'Comprobante #{{orderId}}',
    bodyHtml: '<p>Gracias {{customerName}}</p>',
    active: true,
    updatedAt: '2026-10-28T10:00:00Z'
  },
  {
    id: 3,
    code: 'BUYER_ORDER_STATUS',
    name: 'Cambio de estado',
    description: 'Se envía al comprador',
    subject: 'Tu pedido está {{statusLabel}}',
    bodyHtml: '<p>Estado {{statusLabel}}</p>',
    active: true,
    updatedAt: '2026-10-28T10:00:00Z'
  }
];

describe('AdminEmailTemplatesComponent', () => {
  const adminStub = {
    listEmailTemplates: vi.fn(() => of(TEMPLATES)),
    updateEmailTemplate: vi.fn((code, request) => of({
      ...TEMPLATES.find(template => template.code === code)!,
      ...request
    })),
    resetEmailTemplate: vi.fn(() => of(TEMPLATES[0])),
    previewEmailTemplate: vi.fn(() => of({
      subject: 'Nuevo pedido #1024',
      bodyHtml: '<p>Vista previa</p>'
    }))
  };

  beforeEach(() => {
    adminStub.listEmailTemplates.mockClear();
    adminStub.updateEmailTemplate.mockClear();
    adminStub.resetEmailTemplate.mockClear();
    adminStub.previewEmailTemplate.mockClear();
  });

  async function setup(): Promise<AdminEmailTemplatesComponent> {
    await TestBed.configureTestingModule({
      imports: [AdminEmailTemplatesComponent],
      providers: [{ provide: AdminService, useValue: adminStub }]
    }).compileComponents();

    const fixture = TestBed.createComponent(AdminEmailTemplatesComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('carga las plantillas y selecciona la primera', async () => {
    const component = await setup();

    expect(component.templates().length).toBe(3);
    expect(component.selectedCode()).toBe('SELLER_NEW_ORDER');
    expect(component.form.controls.subject.value).toBe('Nuevo pedido #{{orderId}}');
  });

  it('cambia de plantilla al seleccionarla', async () => {
    const component = await setup();

    component.select(TEMPLATES[2]);

    expect(component.selectedCode()).toBe('BUYER_ORDER_STATUS');
    expect(component.form.controls.subject.value).toContain('{{statusLabel}}');
  });

  it('guarda los cambios y muestra confirmación', async () => {
    const component = await setup();
    component.form.patchValue({ subject: 'Asunto nuevo' });

    component.save();

    expect(adminStub.updateEmailTemplate).toHaveBeenCalledWith(
      'SELLER_NEW_ORDER',
      expect.objectContaining({ subject: 'Asunto nuevo' })
    );
    expect(component.okMessage()).toContain('Plantilla guardada');
  });

  it('restaura la plantilla predeterminada', async () => {
    const component = await setup();

    component.reset();

    expect(adminStub.resetEmailTemplate).toHaveBeenCalledWith('SELLER_NEW_ORDER');
    expect(component.okMessage()).toContain('restaurada');
  });

  it('genera la vista previa con el asunto renderizado', async () => {
    const component = await setup();

    component.showPreview();

    expect(adminStub.previewEmailTemplate).toHaveBeenCalledWith('SELLER_NEW_ORDER');
    expect(component.previewSubject()).toBe('Nuevo pedido #1024');
    expect(component.preview()).not.toBeNull();
  });
});
