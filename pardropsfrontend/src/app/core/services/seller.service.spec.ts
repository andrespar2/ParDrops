/**
 * @file src/app/core/services/seller.service.spec.ts
 * @description Pruebas del servicio del panel del vendedor.
 */

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { SellerService } from './seller.service';
import { SellerDashboard, SellerOrder } from '../models/seller.model';

const DASHBOARD_URL = 'http://localhost:8080/api/seller/dashboard';
const PRODUCTS_URL = 'http://localhost:8080/api/seller/products';
const ORDERS_URL = 'http://localhost:8080/api/seller/orders';
const APPLICATIONS_URL = 'http://localhost:8080/api/seller-applications';

/**
 * Crea un resumen de panel de prueba.
 *
 * @returns el resumen listo para usar.
 */
function buildDashboard(): SellerDashboard {
  return {
    storeName: 'Estilo Urbano',
    storeSlug: 'estilo-urbano',
    salesCount: 2,
    orderCount: 5,
    pendingOrderCount: 3,
    publishedProductCount: 4,
    outOfStockCount: 1,
    revenue: 250000,
    recentNotifications: []
  };
}

/**
 * Crea un subpedido de prueba.
 *
 * @param id identificador.
 * @returns el subpedido listo para usar.
 */
function buildOrder(id: number): SellerOrder {
  return {
    id,
    orderId: 10,
    storeName: 'Estilo Urbano',
    customerName: 'Cliente',
    customerPhone: '3000000000',
    addressStreet: 'Calle 1',
    addressCity: 'Bogotá',
    addressPostalCode: '110111',
    addressCountry: 'Colombia',
    status: 'PREPARED',
    subtotal: 100000,
    shipping: 0,
    total: 100000,
    items: [],
    createdAt: '2026-01-01T10:00:00Z'
  };
}

describe('SellerService', () => {
  let service: SellerService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(SellerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('carga el resumen del vendedor autenticado', () => {
    let sales = 0;
    service.getDashboard().subscribe(summary => (sales = summary.salesCount));

    const request = httpMock.expectOne(DASHBOARD_URL);
    expect(request.request.method).toBe('GET');
    request.flush(buildDashboard());

    expect(sales).toBe(2);
  });

  it('crea un producto en el catálogo propio', () => {
    service.createProduct({
      name: 'Camiseta',
      slug: 'camiseta',
      description: 'Camiseta de algodón',
      price: 50000,
      stock: 10
    }).subscribe();

    const request = httpMock.expectOne(PRODUCTS_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.name).toBe('Camiseta');
    request.flush({ id: 'p1', name: 'Camiseta', slug: 'camiseta' });
  });

  it('publica u oculta un producto propio por su identificador', () => {
    service.setProductVisibility('p1', false).subscribe();

    const request = httpMock.expectOne(
      `${PRODUCTS_URL}/p1/visibility?active=false`);
    expect(request.request.method).toBe('PATCH');
    request.flush({ id: 'p1', active: false });
  });

  it('cambia el estado de una parte de pedido propia', () => {
    service.updateOrderStatus(5, 'SHIPPED').subscribe();

    const request = httpMock.expectOne(`${ORDERS_URL}/5/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body.status).toBe('SHIPPED');
    request.flush({ ...buildOrder(5), status: 'SHIPPED' });
  });

  it('envía una solicitud para convertirse en vendedor', () => {
    service.submitApplication({
      businessName: 'Nuevo negocio',
      city: 'Medellín'
    }).subscribe();

    const request = httpMock.expectOne(APPLICATIONS_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.businessName).toBe('Nuevo negocio');
    request.flush({ id: 1, status: 'PENDING', businessName: 'Nuevo negocio' });
  });

  it('consulta la solicitud propia y distingue el 404 de "sin solicitud"', () => {
    let status: number | undefined;
    service.getMyApplication().subscribe({
      error: error => (status = error.status)
    });

    httpMock.expectOne(`${APPLICATIONS_URL}/me`).flush(
      { message: 'No tienes solicitudes' },
      { status: 404, statusText: 'Not Found' }
    );

    expect(status).toBe(404);
  });
});
