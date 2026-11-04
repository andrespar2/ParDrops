/**
 * @file src/app/core/services/checkout.service.spec.ts
 * @description Pruebas del servicio de checkout con Wompi.
 */

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { CheckoutService } from './checkout.service';
import { CheckoutSession, CheckoutStatus } from '../models/checkout.model';

const CHECKOUT_URL = 'http://localhost:8080/api/checkout';

const SESSION: CheckoutSession = {
  orderId: 12,
  reference: 'PD-12-ABCDEF',
  amountInCents: 5_870_000,
  currency: 'COP',
  publicKey: 'pub_test_demo',
  integritySignature: 'firma',
  redirectUrl: 'http://localhost:4200/compra/PD-12-ABCDEF',
  environment: 'test',
  customerName: 'Ana',
  customerEmail: 'ana@correo.com',
  customerPhone: '3000000000',
  subtotal: 50_000,
  shipping: 8_000,
  serviceFee: 700,
  total: 58_700,
  items: [{
    productId: 'p-1',
    productName: 'Producto',
    unitPrice: 50_000,
    quantity: 1,
    subtotal: 50_000
  }]
};

describe('CheckoutService', () => {
  let service: CheckoutService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CheckoutService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('crea el checkout con los datos del carrito', () => {
    service.createSession({
      customerName: 'Ana',
      customerEmail: 'ana@correo.com',
      customerPhone: '3000000000',
      addressStreet: 'Calle 1',
      addressCity: 'Bogotá',
      addressPostalCode: '110111',
      addressCountry: 'Colombia',
      notes: null,
      items: [{ productId: 'p-1', quantity: 1 }]
    }).subscribe(session => expect(session.reference).toBe('PD-12-ABCDEF'));

    const request = httpMock.expectOne(CHECKOUT_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.items.length).toBe(1);
    request.flush(SESSION);
  });

  it('consulta el estado con el identificador de la transacción', () => {
    service.getStatus('PD-12-ABCDEF', 'tx-1').subscribe(status => {
      expect(status.paymentStatus).toBe('APPROVED');
    });

    const request = httpMock.expectOne(
      `${CHECKOUT_URL}/PD-12-ABCDEF?transactionId=tx-1`
    );
    expect(request.request.method).toBe('GET');
    request.flush({
      reference: 'PD-12-ABCDEF',
      order: {
        id: 12,
        customerName: 'Ana',
        customerEmail: '',
        customerPhone: '',
        addressStreet: '',
        addressCity: '',
        addressPostalCode: '',
        addressCountry: '',
        notes: null,
        items: [],
        subtotal: 50_000,
        shipping: 8_000,
        total: 58_700,
        status: 'PAID',
        createdAt: '2026-01-01T10:00:00Z',
        paymentReference: 'PD-12-ABCDEF',
        paymentStatus: 'APPROVED',
        serviceFee: 700
      },
      paymentStatus: 'APPROVED',
      verifiedPayment: true,
      canRetry: false,
      message: 'Pago confirmado',
      checkedAt: '2026-01-01T10:00:00Z'
    } satisfies CheckoutStatus);
  });

  it('regenera la referencia al reintentar un pago rechazado', () => {
    service.retry('PD-12-ABCDEF').subscribe(session => {
      expect(session.reference).toBe('PD-12-NUEVA');
    });

    const request = httpMock.expectOne(`${CHECKOUT_URL}/PD-12-ABCDEF/retry`);
    expect(request.request.method).toBe('POST');
    request.flush({ ...SESSION, reference: 'PD-12-NUEVA' });
  });
});
