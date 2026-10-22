/**
 * @file src/app/pages/notifications/notifications.component.ts
 * @description Página dedicada de notificaciones.
 *
 * Lista todos los avisos del usuario autenticado, permite marcar uno o todos
 * como leídos y enlaza con la pantalla que corresponde a cada aviso.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationService } from '../../core/services/notification.service';
import { formatDateTime } from '../../core/config/commerce.config';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.scss']
})
export class NotificationsComponent {
  /** Servicio de notificaciones. */
  private readonly notificationService = inject(NotificationService);

  /** Router para navegar al aviso pulsado. */
  private readonly router = inject(Router);

  /** Avisos del usuario. */
  public readonly notifications = this.notificationService.notifications;

  /** Número de avisos sin leer. */
  public readonly unreadCount = this.notificationService.unreadCount;

  /** Indica si los avisos se están cargando. */
  public readonly loading = signal<boolean>(true);

  /** Indica si hubo un error al cargar los avisos. */
  public readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.notificationService.loadNotifications().subscribe({
      next: () => this.loading.set(false),
      error: () => {
        this.errorMessage.set('No se pudieron cargar tus notificaciones.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Marca un aviso como leído y abre la pantalla que indica.
   *
   * @param id identificador del aviso.
   * @param link ruta interna del aviso (opcional).
   * @returns {void}
   */
  public openNotification(id: number, link?: string | null): void {
    this.notificationService.markAsRead(id).subscribe({
      error: () => {
        /* Si el aviso ya no existe, la lista se vuelve a consultar. */
        this.reload();
      }
    });

    if (link) {
      this.router.navigateByUrl(link);
    }
  }

  /**
   * Marca todos los avisos como leídos.
   *
   * @returns {void}
   */
  public markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      error: () => this.reload()
    });
  }

  /**
   * Vuelve a cargar la lista de avisos.
   *
   * @returns {void}
   */
  private reload(): void {
    this.notificationService.loadNotifications().subscribe({
      error: () => this.errorMessage.set('No se pudieron cargar tus notificaciones.')
    });
  }

  /**
   * Formatea la fecha de un aviso.
   *
   * @param isoDate fecha en formato ISO.
   * @returns la fecha formateada en español.
   */
  public formatDate(isoDate: string): string {
    return formatDateTime(isoDate);
  }
}
