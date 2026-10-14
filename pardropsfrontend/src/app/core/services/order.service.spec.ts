/**
 * @file src/app/core/services/order.service.spec.ts
 * @description Pruebas del servicio de pedidos: estados y aviso de pedidos nuevos.
 */

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { OrderService } from './order.service';
import { Order, OrderStatus } from '../models/order.model';

const ORDERS_URL = 'http://localhost:8080/api/orders';

/**
 * Crea un pedido de prueba.
 *
 * @param id     identificador del pedido.
 * @param status estado inicial.
 * @returns el pedido listo para usar en las pruebas.
 */
function buildOrder(id: number, status: OrderStatus = 'PENDING'): Order {
  return {
    id,
    customerName: `Cliente ${id}`,
    customerEmail: `cliente${id}@pardrops.com`,
    customerPhone: '+34 600 000 000',
    addressStreet: 'Calle Mayor 1',
    addressCity: 'Madrid',
    addressPostalCode: '28001',
    addressCountry: 'España',
    notes: null,
    items: [],
    subtotal: 10,
    tax: 2.1,
    shipping: 4.99,
    total: 17.09,
    status,
    createdAt: '2026-01-01T10:00:00Z',
    userEmail: null
  };
}

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    service.stopNotificationPolling();
    httpMock.verify();
    vi.useRealTimers();
  });

  it('avisa de los pedidos que llegan durante el sondeo', () => {
    vi.useFakeTimers();

    service.startNotificationPolling();

    // Primera lectura: solo registra los pedidos que ya existían.
    httpMock.expectOne(ORDERS_URL).flush([buildOrder(1)]);
    expect(service.newArrivals().length).toBe(0);

    // Llega una compra nueva.
    vi.advanceTimersByTime(15000);
    httpMock.expectOne(ORDERS_URL).flush([buildOrder(2), buildOrder(1)]);

    expect(service.newArrivals().map(order => order.id)).toEqual([2]);
    expect(service.pendingCount()).toBe(2);
    expect(service.pendingOrders().length).toBe(2);
  });

  it('deja de avisar cuando el administrador marca los pedidos como vistos', () => {
    vi.useFakeTimers();

    service.startNotificationPolling();
    httpMock.expectOne(ORDERS_URL).flush([]);

    vi.advanceTimersByTime(15000);
    httpMock.expectOne(ORDERS_URL).flush([buildOrder(7)]);
    expect(service.newArrivals().length).toBe(1);

    service.acknowledgeNewOrders();
    expect(service.newArrivals().length).toBe(0);
  });

  it('cambia el estado de un pedido y actualiza la lista local', () => {
    service.loadAllOrders().subscribe();
    httpMock.expectOne(ORDERS_URL).flush([buildOrder(3)]);

    service.updateOrderStatus(3, 'PREPARED').subscribe();
    const request = httpMock.expectOne(`${ORDERS_URL}/3/status`);

    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'PREPARED' });

    request.flush(buildOrder(3, 'PREPARED'));

    expect(service.allOrders()[0].status).toBe('PREPARED');
    expect(service.pendingCount()).toBe(0);
  });

  it('solo cuenta como pendientes los pedidos por gestionar', () => {
    service.loadAllOrders().subscribe();

    httpMock
      .expectOne(ORDERS_URL)
      .flush([buildOrder(4, 'PENDING'), buildOrder(5, 'SHIPPED'), buildOrder(6, 'DELIVERED')]);

    expect(service.pendingCount()).toBe(1);
    expect(service.pendingOrders()[0].id).toBe(4);
  });

  it('no sondea si ya está activo', () => {
    vi.useFakeTimers();

    service.startNotificationPolling();
    httpMock.expectOne(ORDERS_URL).flush([]);

    // Una segunda llamada no debe abrir otro sondeo.
    service.startNotificationPolling();
    httpMock.expectNone(ORDERS_URL);
  });
});
