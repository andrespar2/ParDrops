/**
 * @file src/app/core/services/notification.service.ts
 * @description Servicio de las notificaciones dentro de la aplicación.
 *
 * Centraliza el acceso a los avisos del usuario: listado, contador de no
 * leídas y marcado. El estado se expone con signals para que la campanilla de
 * la cabecera y la página de notificaciones reaccione a los cambios.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Notification, UnreadCount } from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** URL base de los endpoints de notificaciones. */
  private readonly notificationsUrl = `${API_BASE_URL}/notifications`;

  /** Signal con la lista de avisos del usuario autenticado. */
  private readonly _notifications = signal<Notification[]>([]);

  /** Signal con el número de avisos sin leer. */
  private readonly _unreadCount = signal<number>(0);

  /** Avisos del usuario autenticado (solo lectura). */
  public readonly notifications = this._notifications.asReadonly();

  /** Número de avisos sin leer (solo lectura). */
  public readonly unreadCount = this._unreadCount.asReadonly();

  /** Avisos que todavía no se han leído (solo lectura). */
  public readonly unreadNotifications = computed(() =>
    this._notifications().filter(notification => !notification.read)
  );

  /**
   * Carga la lista de avisos del usuario autenticado.
   *
   * @returns observable con la lista de avisos.
   */
  public loadNotifications(): Observable<Notification[]> {
    return this.http
      .get<Notification[]>(this.notificationsUrl)
      .pipe(tap(notifications => this._notifications.set(notifications)));
  }

  /**
   * Consulta el número de avisos sin leer.
   *
   * @returns observable con el contador.
   */
  public loadUnreadCount(): Observable<UnreadCount> {
    return this.http
      .get<UnreadCount>(`${this.notificationsUrl}/unread-count`)
      .pipe(tap(count => this._unreadCount.set(count.unreadCount)));
  }

  /**
   * Marca un aviso como leído.
   *
   * @param id identificador del aviso.
   * @returns observable con el aviso actualizado.
   */
  public markAsRead(id: number): Observable<Notification> {
    return this.http
      .patch<Notification>(`${this.notificationsUrl}/${id}/read`, {})
      .pipe(tap(notification => this.replaceNotification(notification)));
  }

  /**
   * Marca todos los avisos como leídos.
   *
   * @returns observable con el contador de avisos que quedan sin leer.
   */
  public markAllAsRead(): Observable<UnreadCount> {
    return this.http
      .patch<UnreadCount>(`${this.notificationsUrl}/read-all`, {})
      .pipe(
        tap(() => {
          this._notifications.update(notifications =>
            notifications.map(notification => ({ ...notification, read: true }))
          );
          this._unreadCount.set(0);
        })
      );
  }

  /**
   * Limpia los avisos guardados en memoria.
   *
   * Se usa al cerrar la sesión, para que el siguiente usuario no vea los
   * avisos del anterior.
   *
   * @returns {void}
   */
  public reset(): void {
    this._notifications.set([]);
    this._unreadCount.set(0);
  }

  /**
   * Sustituye un aviso de la lista local por su versión actualizada.
   *
   * @param notification aviso devuelto por la API.
   * @returns {void}
   * @private
   */
  private replaceNotification(notification: Notification): void {
    this._notifications.update(notifications =>
      notifications.map(item => (item.id === notification.id ? notification : item))
    );
    this._unreadCount.set(
      this._notifications().filter(item => !item.read).length
    );
  }
}
