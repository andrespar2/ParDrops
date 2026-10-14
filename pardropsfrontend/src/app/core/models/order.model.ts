/**
 * @file src/app/core/models/order.model.ts
 * @description Modelos relacionados con los pedidos.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

/**
 * Estados posibles de un pedido.
 *
 * El flujo habitual es `PENDING` → `PAID` → `PREPARED` → `SHIPPED` →
 * `DELIVERED`. `DELIVERED` y `CANCELLED` son estados finales.
 */
export type OrderStatus = 'PENDING' | 'PAID' | 'PREPARED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

/** Etiquetas legibles (en español) para cada estado de pedido. */
export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pendiente',
  PAID: 'Pagado',
  PREPARED: 'Preparado',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado'
};

/**
 * Estados a los que se puede pasar desde cada estado.
 *
 * Refleja las transiciones que valida el backend: solo se puede avanzar en el
 * flujo de preparación o cancelar antes de la entrega.
 */
export const ORDER_STATUS_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['PAID', 'PREPARED', 'SHIPPED', 'CANCELLED'],
  PAID: ['PREPARED', 'SHIPPED', 'CANCELLED'],
  PREPARED: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED', 'CANCELLED'],
  DELIVERED: [],
  CANCELLED: []
};

/**
 * Devuelve la etiqueta legible de un estado de pedido.
 *
 * @param status estado del pedido.
 * @returns la etiqueta en español.
 */
export function orderStatusLabel(status: OrderStatus): string {
  return ORDER_STATUS_LABELS[status];
}

/**
 * Devuelve los estados a los que se puede pasar desde el estado indicado.
 *
 * @param status estado actual del pedido.
 * @returns lista de estados destino permitidos.
 */
export function allowedOrderTransitions(status: OrderStatus): OrderStatus[] {
  return ORDER_STATUS_TRANSITIONS[status];
}

/**
 * Línea de un pedido tal y como la devuelve la API.
 *
 * @interface OrderItem
 */
export interface OrderItem {
  /** Identificador del producto comprado. */
  productId: string;
  /** Nombre del producto en el momento de la compra. */
  productName: string;
  /** Slug del producto (para enlazar de nuevo al detalle). */
  productSlug?: string | null;
  /** Nombre de la variante elegida, si la hubiera. */
  variantName?: string | null;
  /** Precio unitario aplicado. */
  unitPrice: number;
  /** Unidades compradas. */
  quantity: number;
  /** Importe de la línea. */
  subtotal: number;
}

/**
 * Pedido completo devuelto por la API.
 *
 * @interface Order
 */
export interface Order {
  /** Identificador del pedido. */
  id: number;
  /** Nombre de quien recibe el pedido. */
  customerName: string;
  /** Correo de contacto. */
  customerEmail: string;
  /** Teléfono de contacto. */
  customerPhone: string;
  /** Calle y número. */
  addressStreet: string;
  /** Ciudad. */
  addressCity: string;
  /** Código postal. */
  addressPostalCode: string;
  /** País. */
  addressCountry: string;
  /** Comentarios del cliente. */
  notes?: string | null;
  /** Líneas del pedido. */
  items: OrderItem[];
  /** Suma de las líneas. */
  subtotal: number;
  /** Impuestos aplicados. */
  tax: number;
  /** Gastos de envío. */
  shipping: number;
  /** Importe total. */
  total: number;
  /** Estado actual. */
  status: OrderStatus;
  /** Fecha de creación (ISO 8601). */
  createdAt: string;
  /** Correo de la cuenta asociada (nulo si fue una compra de invitado). */
  userEmail?: string | null;
}

/**
 * Línea enviada al registrar un pedido.
 *
 * @interface CreateOrderItemRequest
 */
export interface CreateOrderItemRequest {
  /** Producto comprado. */
  productId: string;
  /** Slug del producto (informativo). */
  productSlug?: string | null;
  /** Variante elegida (opcional). */
  variantName?: string | null;
  /** Unidades compradas. */
  quantity: number;
}

/**
 * Datos enviados al registrar un pedido (formulario de checkout).
 *
 * @interface CreateOrderRequest
 */
export interface CreateOrderRequest {
  /** Nombre de quien recibe el pedido. */
  customerName: string;
  /** Correo de contacto. */
  customerEmail: string;
  /** Teléfono de contacto. */
  customerPhone: string;
  /** Calle y número. */
  addressStreet: string;
  /** Ciudad. */
  addressCity: string;
  /** Código postal. */
  addressPostalCode: string;
  /** País. */
  addressCountry: string;
  /** Comentarios adicionales (opcional). */
  notes?: string | null;
  /** Líneas del pedido. */
  items: CreateOrderItemRequest[];
}
