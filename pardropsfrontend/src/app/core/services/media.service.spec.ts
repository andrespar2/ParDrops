/**
 * @file src/app/core/services/media.service.spec.ts
 * @description Pruebas del servicio de subida de imagenes.
 */

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { MediaService } from './media.service';
import { MediaAsset } from '../models/media.model';

const MEDIA_URL = 'http://localhost:8080/api/media';

/**
 * Crea la ficha de una imagen de prueba.
 *
 * @param id identificador de la imagen.
 * @returns la imagen lista para usar en las pruebas.
 */
function buildAsset(id: number): MediaAsset {
  return {
    id,
    url: `http://localhost:8080/media/imagen-${id}.png`,
    path: `/media/imagen-${id}.png`,
    originalName: 'producto.png',
    mimeType: 'image/png',
    sizeBytes: 1024,
    createdAt: '2026-01-01T10:00:00Z'
  };
}

describe('MediaService', () => {
  let service: MediaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(MediaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('sube la imagen como multipart y devuelve su URL publica', () => {
    const file = new File(['contenido'], 'producto.png', { type: 'image/png' });

    let recibida: MediaAsset | undefined;
    service.upload(file).subscribe(asset => (recibida = asset));

    const request = httpMock.expectOne(MEDIA_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body instanceof FormData).toBe(true);
    expect((request.request.body as FormData).get('file')).toBe(file);
    request.flush(buildAsset(1));

    expect(recibida?.url).toContain('/media/');
  });

  it('lista las imagenes del usuario autenticado', () => {
    let total = 0;
    service.listMine().subscribe(assets => (total = assets.length));

    const request = httpMock.expectOne(MEDIA_URL);
    expect(request.request.method).toBe('GET');
    request.flush([buildAsset(1), buildAsset(2)]);

    expect(total).toBe(2);
  });

  it('elimina una imagen por su identificador', () => {
    service.delete(5).subscribe();

    const request = httpMock.expectOne(`${MEDIA_URL}/5`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });

  it('propaga el mensaje de error del servidor', () => {
    const file = new File(['texto'], 'trampa.png', { type: 'image/png' });
    let mensaje: string | undefined;

    service.upload(file).subscribe({
      error: (error) => (mensaje = error.error?.message)
    });

    const request = httpMock.expectOne(MEDIA_URL);
    request.flush(
      { message: 'El fichero no es una imagen valida' },
      { status: 400, statusText: 'Bad Request' }
    );

    expect(mensaje).toBe('El fichero no es una imagen valida');
  });
});
