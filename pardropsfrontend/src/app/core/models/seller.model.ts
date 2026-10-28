/**
 * @file src/app/core/models/seller.model.ts
 * @description Modelos del panel privado del vendedor y de las solicitudes
 * para convertirse en vendedor.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Notification } from './notification.model';
import { OrderItem, OrderStatus } from './order.model';

/**
 * Resumen de actividad que devuelve `GET /api/seller/dashboard`.
 *
 * @interface SellerDashboard
 */
export interface SellerDashboard {
  /** Nombre comercial de la tienda (nulo si aún no existe). */
  storeName?: string | null;
  /** Slug público de la tienda (nulo si aún no existe). */
  storeSlug?: string | null;
  /** Subpedidos entregados. */
  salesCount: number;
  /** Subpedidos recibidos. */
  orderCount: number;
  /** Subpedidos que todavía no han finalizado. */
  pendingOrderCount: number;
  /** Productos publicados. */
  publishedProductCount: number;
  /** Productos publicados sin existencias. */
  outOfStockCount: number;
  /** Ingresos acumulados de los subpedidos no cancelados. */
  revenue: number;
  /** Comisión acumulada de ParDrops. */
  commissionTotal: number;
  /** Neto acumulado de las ventas (retenido + disponible). */
  netRevenue: number;
  /** Neto retenido hasta la entrega. */
  pendingBalance: number;
  /** Neto disponible para el vendedor. */
  availableBalance: number;
  /** Últimos avisos recibidos. */
  recentNotifications: Notification[];
}

/**
 * Parte de un pedido que corresponde al vendedor autenticado.
 *
 * @interface SellerOrder
 */
export interface SellerOrder {
  /** Identificador del subpedido. */
  id: number;
  /** Pedido al que pertenece. */
  orderId: number;
  /** Nombre de la tienda. */
  storeName: string;
  /** Nombre de quien recibe el pedido. */
  customerName: string;
  /** Teléfono de contacto del cliente. */
  customerPhone?: string | null;
  /** Calle y número. */
  addressStreet: string;
  /** Ciudad. */
  addressCity: string;
  /** Código postal. */
  addressPostalCode: string;
  /** País. */
  addressCountry: string;
  /** Estado del subpedido. */
  status: OrderStatus;
  /** Suma de las líneas. */
  subtotal: number;
  /** Gastos de envío. */
  shipping: number;
  /** Importe total. */
  total: number;
  /** Líneas del subpedido. */
  items: OrderItem[];
  /** Fecha de creación (ISO 8601). */
  createdAt: string;
}

/** Estado de una solicitud para vender. */
export type SellerApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

/**
 * Solicitud para convertirse en vendedor.
 *
 * @interface SellerApplication
 */
export interface SellerApplication {
  /** Identificador de la solicitud. */
  id: number;
  /** Nombre comercial del negocio. */
  businessName: string;
  /** NIT o identificación tributaria. */
  taxId?: string | null;
  /** Descripción del negocio. */
  description?: string | null;
  /** Teléfono de contacto. */
  phone?: string | null;
  /** Ciudad del negocio. */
  city?: string | null;
  /** Estado de la solicitud. */
  status: SellerApplicationStatus;
  /** Comentario del administrador. */
  reviewNotes?: string | null;
  /** Nombre del solicitante. */
  applicantName: string;
  /** Correo del solicitante. */
  applicantEmail: string;
  /** Administrador que revisó la solicitud. */
  reviewedBy?: string | null;
  /** Fecha de revisión (ISO 8601). */
  reviewedAt?: string | null;
  /** Fecha de envío (ISO 8601). */
  createdAt: string;
}

/**
 * Datos enviados al solicitar una tienda.
 *
 * @interface SellerApplicationRequest
 */
export interface SellerApplicationRequest {
  /** Nombre comercial del negocio. */
  businessName: string;
  /** NIT o identificación tributaria. */
  taxId?: string | null;
  /** Descripción del negocio. */
  description?: string | null;
  /** Teléfono de contacto. */
  phone?: string | null;
  /** Ciudad del negocio. */
  city?: string | null;
}
