/**
 * @file src/app/core/services/store.service.spec.ts
 * @description Pruebas del servicio de tiendas públicas y de la ficha propia.
 */

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { StoreService } from './store.service';
import { Store } from '../models/store.model';

const STORES_URL = 'http://localhost:8080/api/stores';

/**
 * Crea una tienda de prueba.
 *
 * @param slug slug público.
 * @returns la tienda lista para usar.
 */
function buildStore(slug: string): Store {
  return {
    id: 1,
    name: 'Estilo Urbano',
    slug,
    description: 'Moda urbana',
    logoUrl: null,
    bannerUrl: null,
    contactEmail: 'hola@estilourbano.com',
    contactPhone: '3001112222',
    whatsapp: null,
    status: 'ACTIVE',
    sellerName: 'Vendedor de ejemplo',
    productCount: 3,
    createdAt: '2026-01-01T10:00:00Z'
  };
}

describe('StoreService', () => {
  let service: StoreService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(StoreService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('consulta la ficha pública de una tienda por su slug', () => {
    let store: Store | undefined;
    service.getBySlug('estilo-urbano').subscribe(result => (store = result));

    const request = httpMock.expectOne(`${STORES_URL}/estilo-urbano`);
    expect(request.request.method).toBe('GET');
    request.flush(buildStore('estilo-urbano'));

    expect(store?.name).toBe('Estilo Urbano');
  });

  it('consulta solo los productos publicados de la tienda', () => {
    let total = 0;
    service.getProducts('estilo-urbano').subscribe(products => (total = products.length));

    const request = httpMock.expectOne(`${STORES_URL}/estilo-urbano/products`);
    expect(request.request.method).toBe('GET');
    request.flush([{ id: 'p1', name: 'Camiseta' }, { id: 'p2', name: 'Gorra' }]);

    expect(total).toBe(2);
  });

  it('carga la tienda del vendedor autenticado', () => {
    service.getMine().subscribe();

    const request = httpMock.expectOne(`${STORES_URL}/mine`);
    expect(request.request.method).toBe('GET');
    request.flush(buildStore('estilo-urbano'));
  });

  it('actualiza la ficha propia con el nombre y el slug', () => {
    let slug: string | undefined;

    service.updateMine({
      name: 'Moda Urbana',
      slug: 'moda-urbana',
      description: 'Nueva descripción'
    }).subscribe(store => (slug = store.slug));

    const request = httpMock.expectOne(`${STORES_URL}/mine`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.slug).toBe('moda-urbana');
    request.flush({ ...buildStore('moda-urbana'), name: 'Moda Urbana' });

    expect(slug).toBe('moda-urbana');
  });

  it('propaga el conflicto de slug duplicado del servidor', () => {
    let status: number | undefined;

    service.updateMine({ slug: 'moda-urbana' }).subscribe({
      error: error => (status = error.status)
    });

    httpMock.expectOne(`${STORES_URL}/mine`).flush(
      { message: 'Ya existe otra tienda con el slug: moda-urbana' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(status).toBe(409);
  });
});
