/**
 * @file src/app/core/services/notification.service.spec.ts
 * @description Pruebas del servicio de notificaciones: listado, contador y marcado.
 */

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { NotificationService } from './notification.service';
import { Notification } from '../models/notification.model';

const NOTIFICATIONS_URL = 'http://localhost:8080/api/notifications';

/**
 * Crea una notificación de prueba.
 *
 * @param id   identificador de la notificación.
 * @param read indica si ya fue leída.
 * @returns la notificación lista para usar en las pruebas.
 */
function buildNotification(id: number, read = false): Notification {
  return {
    id,
    type: 'ORDER',
    title: 'Tienes una venta nueva',
    message: 'La tienda tiene un pedido por preparar.',
    link: '/vendedor/pedidos',
    read,
    createdAt: '2026-01-01T10:00:00Z'
  };
}

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('carga los avisos del usuario y el contador de no leídos', () => {
    service.loadNotifications().subscribe();
    httpMock.expectOne(NOTIFICATIONS_URL).flush([buildNotification(1), buildNotification(2, true)]);

    service.loadUnreadCount().subscribe();
    httpMock.expectOne(`${NOTIFICATIONS_URL}/unread-count`).flush({ unreadCount: 1 });

    expect(service.notifications().length).toBe(2);
    expect(service.unreadCount()).toBe(1);
    expect(service.unreadNotifications().map(item => item.id)).toEqual([1]);
  });

  it('el usuario solo ve sus propios avisos', () => {
    service.loadNotifications().subscribe();

    const request = httpMock.expectOne(NOTIFICATIONS_URL);
    expect(request.request.method).toBe('GET');
    request.flush([buildNotification(1)]);

    expect(service.notifications().length).toBe(1);
  });

  it('actualiza el contador al marcar un aviso como leído', () => {
    service.loadNotifications().subscribe();
    httpMock.expectOne(NOTIFICATIONS_URL).flush([buildNotification(1), buildNotification(2)]);
    service.loadUnreadCount().subscribe();
    httpMock.expectOne(`${NOTIFICATIONS_URL}/unread-count`).flush({ unreadCount: 2 });

    service.markAsRead(1).subscribe();
    const request = httpMock.expectOne(`${NOTIFICATIONS_URL}/1/read`);
    expect(request.request.method).toBe('PATCH');
    request.flush(buildNotification(1, true));

    expect(service.unreadCount()).toBe(1);
    expect(service.notifications()[0].read).toBe(true);
  });

  it('deja el contador en cero al marcar todos los avisos como leídos', () => {
    service.loadNotifications().subscribe();
    httpMock.expectOne(NOTIFICATIONS_URL).flush([buildNotification(1), buildNotification(2)]);

    service.markAllAsRead().subscribe();
    const request = httpMock.expectOne(`${NOTIFICATIONS_URL}/read-all`);
    expect(request.request.method).toBe('PATCH');
    request.flush({ unreadCount: 0 });

    expect(service.unreadCount()).toBe(0);
    expect(service.unreadNotifications().length).toBe(0);
  });
});
