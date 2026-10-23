/**
 * @file src/app/shared/components/notification-bell/notification-bell.component.ts
 * @description Campanilla de notificaciones de la cabecera.
 *
 * Muestra el número de avisos sin leer y, al desplegarla, la lista de los
 * últimos avisos con un enlace directo a la página de notificaciones.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.scss']
})
export class NotificationBellComponent {
  /** Servicio de notificaciones. */
  private readonly notificationService = inject(NotificationService);

  /** Servicio de autenticación. */
  private readonly authService = inject(AuthService);

  /** Router para navegar al aviso pulsado. */
  private readonly router = inject(Router);

  /** Número de avisos sin leer (solo lectura). */
  public readonly unreadCount = this.notificationService.unreadCount;

  /** Avisos del usuario (solo lectura). */
  public readonly notifications = this.notificationService.notifications;

  /** Indica si el desplegable está abierto. */
  public readonly isOpen = signal<boolean>(false);

  constructor() {
    // Con sesión iniciada se consultan los avisos; al cerrarla se limpian.
    effect(() => {
      if (this.authService.isAuthenticated()) {
        this.refresh();
      } else {
        this.notificationService.reset();
      }
    });
  }

  /**
   * Alterna el desplegable de avisos.
   *
   * @returns {void}
   */
  public toggle(): void {
    this.isOpen.update(open => !open);

    if (this.isOpen()) {
      this.refresh();
    }
  }

  /**
   * Cierra el desplegable de avisos.
   *
   * @returns {void}
   */
  public close(): void {
    this.isOpen.set(false);
  }

  /**
   * Marca un aviso como leído y lleva a la pantalla que indica.
   *
   * @param id identificador del aviso.
   * @param link ruta interna del aviso (opcional).
   * @returns {void}
   */
  public openNotification(id: number, link?: string | null): void {
    this.notificationService.markAsRead(id).subscribe({
      error: () => {
        /* Si el aviso ya no existe, el contador se recalcula igual. */
        this.refreshCount();
      }
    });

    this.close();

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
      error: () => this.refreshCount()
    });
  }

  /**
   * Vuelve a consultar los avisos y el contador.
   *
   * @returns {void}
   */
  private refresh(): void {
    this.notificationService.loadNotifications().subscribe({
      error: () => {
        /* La campanilla nunca debe romper la navegación. */
      }
    });
    this.refreshCount();
  }

  /**
   * Vuelve a consultar solo el contador de avisos sin leer.
   *
   * @returns {void}
   */
  private refreshCount(): void {
    this.notificationService.loadUnreadCount().subscribe({
      error: () => {
        /* La campanilla nunca debe romper la navegación. */
      }
    });
  }
}
