/**
 * @file src/app/core/models/notification.model.ts
 * @description Modelos relacionados con las notificaciones dentro de la aplicación.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

/**
 * Tipos de aviso que genera la plataforma.
 * - `INFO`: aviso genérico.
 * - `SELLER_APPLICATION`: resultado de la revisión de una solicitud de vendedor.
 * - `ORDER`: aviso relacionado con un pedido o una parte de un pedido.
 * - `PRODUCT`: aviso de moderación de un producto.
 * - `ACCOUNT`: aviso sobre el estado de la cuenta.
 */
export type NotificationType =
  | 'INFO'
  | 'SELLER_APPLICATION'
  | 'ORDER'
  | 'PRODUCT'
  | 'ACCOUNT';

/**
 * Notificación tal y como la devuelve la API.
 *
 * @interface Notification
 */
export interface Notification {
  /** Identificador de la notificación. */
  id: number;
  /** Tipo de aviso. */
  type: NotificationType;
  /** Título corto del aviso. */
  title: string;
  /** Texto explicativo del aviso. */
  message: string;
  /** Ruta interna a la que lleva el aviso (opcional). */
  link?: string | null;
  /** Indica si el aviso ya fue leído. */
  read: boolean;
  /** Fecha de creación (ISO 8601). */
  createdAt: string;
}

/**
 * Contador de notificaciones pendientes de leer.
 *
 * @interface UnreadCount
 */
export interface UnreadCount {
  /** Cantidad de avisos sin leer. */
  unreadCount: number;
}
