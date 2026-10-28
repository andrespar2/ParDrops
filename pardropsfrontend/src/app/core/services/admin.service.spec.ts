/**
 * @file src/app/core/services/admin.service.spec.ts
 * @description Pruebas del servicio del panel de administracion.
 *
 * Comprueban que cada seccion del panel llama al endpoint real de la API con
 * los parametros correctos: tablero, cuentas con filtros y paginacion, cambio
 * de estado, vendedores, catalogo global con visibilidad, categorias,
 * solicitudes y auditoria.
 */

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AdminService } from './admin.service';
import { CategoryRequest } from '../models/admin.model';

const API = 'http://localhost:8080/api';
const ADMIN = `${API}/admin`;

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('carga el resumen general desde /api/admin/dashboard', () => {
    let total: number | undefined;
    service.loadDashboard().subscribe(data => (total = data.userCount));

    const request = httpMock.expectOne(`${ADMIN}/dashboard`);
    expect(request.request.method).toBe('GET');
    request.flush({
      userCount: 12,
      customerCount: 8,
      sellerCount: 4,
      activeSellerCount: 3,
      suspendedUserCount: 1,
      productCount: 20,
      publishedProductCount: 18,
      outOfStockCount: 2,
      storeCount: 4,
      activeStoreCount: 3,
      orderCount: 7,
      pendingOrderCount: 2,
      revenue: 1500000,
      pendingApplicationCount: 1,
      recentOrders: [],
      topStores: []
    });

    expect(total).toBe(12);
  });

  it('lista cuentas con busqueda, rol, estado y paginacion', () => {
    service.listUsers({
      search: 'ana',
      role: 'SELLER',
      status: 'ACTIVE',
      page: 2,
      size: 5
    }).subscribe();

    const request = httpMock.expectOne(
      req => req.url === `${ADMIN}/users`
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('ana');
    expect(request.request.params.get('role')).toBe('SELLER');
    expect(request.request.params.get('status')).toBe('ACTIVE');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('5');
    request.flush({ content: [], page: 2, size: 5, totalElements: 0, totalPages: 0 });
  });

  it('no envia filtros vacios en el listado de cuentas', () => {
    service.listUsers({ search: '   ' }).subscribe();

    const request = httpMock.expectOne(req => req.url === `${ADMIN}/users`);
    expect(request.request.params.has('search')).toBe(false);
    expect(request.request.params.has('role')).toBe(false);
    expect(request.request.params.has('status')).toBe(false);
    expect(request.request.params.get('page')).toBe('0');
    request.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('cambia el estado de una cuenta con el motivo', () => {
    service.changeUserStatus(7, 'SUSPENDED', 'incumplimiento').subscribe();

    const request = httpMock.expectOne(`${ADMIN}/users/7/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({
      status: 'SUSPENDED',
      reason: 'incumplimiento'
    });
    request.flush({});
  });

  it('lista vendedores filtrando por estado y texto', () => {
    service.listSellers('ACTIVE', 'moda').subscribe();

    const request = httpMock.expectOne(req => req.url === `${ADMIN}/sellers`);
    expect(request.request.params.get('status')).toBe('ACTIVE');
    expect(request.request.params.get('search')).toBe('moda');
    request.flush([]);
  });

  it('lista el catalogo global con visibilidad y vendedor', () => {
    service.listProducts({
      search: 'camisa',
      sellerId: 3,
      category: 'Ropa',
      active: false,
      page: 1,
      size: 10
    }).subscribe();

    const request = httpMock.expectOne(req => req.url === `${ADMIN}/products`);
    expect(request.request.params.get('search')).toBe('camisa');
    expect(request.request.params.get('sellerId')).toBe('3');
    expect(request.request.params.get('category')).toBe('Ropa');
    expect(request.request.params.get('active')).toBe('false');
    expect(request.request.params.get('page')).toBe('1');
    request.flush({ content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('publica u oculta un producto con el parametro active', () => {
    service.changeProductVisibility('abc', false).subscribe();

    const request = httpMock.expectOne(
      req => req.url === `${ADMIN}/products/abc/visibility`
    );
    expect(request.request.method).toBe('PATCH');
    expect(request.request.params.get('active')).toBe('false');
    request.flush({});
  });

  it('elimina un producto del catalogo global', () => {
    service.deleteProduct('abc').subscribe();

    const request = httpMock.expectOne(`${ADMIN}/products/abc`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });

  it('lista las categorias indicando si solo activas', () => {
    service.listCategories(true).subscribe();

    const request = httpMock.expectOne(req => req.url === `${ADMIN}/categories`);
    expect(request.request.params.get('activeOnly')).toBe('true');
    request.flush([]);
  });

  it('crea una categoria con los datos enviados', () => {
    const payload: CategoryRequest = {
      name: 'Calzado',
      slug: 'calzado',
      description: 'Zapatos y zapatillas',
      active: true,
      featured: false
    };

    service.createCategory(payload).subscribe();

    const request = httpMock.expectOne(`${ADMIN}/categories`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({});
  });

  it('actualiza y elimina una categoria por su identificador', () => {
    const payload: CategoryRequest = {
      name: 'Calzado',
      slug: 'calzado',
      description: null,
      active: false,
      featured: true
    };

    service.updateCategory(4, payload).subscribe();
    httpMock.expectOne(`${ADMIN}/categories/4`).flush({});

    service.deleteCategory(4).subscribe();
    const del = httpMock.expectOne(`${ADMIN}/categories/4`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('resuelve una solicitud de vendedor con la decision', () => {
    service.decideApplication(9, { approve: false, reviewNotes: 'datos incompletos' })
      .subscribe();

    const request = httpMock.expectOne(`${API}/seller-applications/9/decision`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({
      approve: false,
      reviewNotes: 'datos incompletos'
    });
    request.flush({});
  });

  it('lista la auditoria de administracion', () => {
    service.listAuditLogs().subscribe();

    const request = httpMock.expectOne(`${ADMIN}/audit-logs`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('lista y consulta las plantillas de correo', () => {
    service.listEmailTemplates().subscribe();
    const list = httpMock.expectOne(`${ADMIN}/email-templates`);
    expect(list.request.method).toBe('GET');
    list.flush([]);

    service.getEmailTemplate('SELLER_NEW_ORDER').subscribe();
    const one = httpMock.expectOne(`${ADMIN}/email-templates/SELLER_NEW_ORDER`);
    expect(one.request.method).toBe('GET');
    one.flush({});
  });

  it('actualiza una plantilla con asunto, cuerpo y estado', () => {
    service.updateEmailTemplate('BUYER_ORDER_STATUS', {
      subject: 'Nuevo asunto',
      bodyHtml: '<p>Hola</p>',
      active: true
    }).subscribe();

    const request = httpMock.expectOne(`${ADMIN}/email-templates/BUYER_ORDER_STATUS`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      subject: 'Nuevo asunto',
      bodyHtml: '<p>Hola</p>',
      active: true
    });
    request.flush({});
  });

  it('restaura y previsualiza una plantilla', () => {
    service.resetEmailTemplate('BUYER_PAYMENT_RECEIPT').subscribe();
    const reset = httpMock.expectOne(`${ADMIN}/email-templates/BUYER_PAYMENT_RECEIPT/reset`);
    expect(reset.request.method).toBe('POST');
    reset.flush({});

    service.previewEmailTemplate('BUYER_PAYMENT_RECEIPT').subscribe();
    const preview = httpMock.expectOne(`${ADMIN}/email-templates/BUYER_PAYMENT_RECEIPT/preview`);
    expect(preview.request.method).toBe('POST');
    preview.flush({ subject: 'Comprobante', bodyHtml: '<p>Hola</p>' });
  });
});
