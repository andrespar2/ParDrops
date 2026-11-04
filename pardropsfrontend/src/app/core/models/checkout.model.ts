/**
 * @file src/app/core/models/checkout.model.ts
 * @description Modelos del checkout con Wompi y de la pantalla de resultado.
 */

import { Order, PaymentStatus } from './order.model';
/**
 * Datos que el backend devuelve para abrir el widget de Wompi.
 *
 * @interface CheckoutSession
 */
export interface CheckoutSession {
  /** Pedido creado en estado pendiente de pago. */
  orderId: number;
  /** Referencia única del pago. */
  reference: string;
  /** Monto en centavos. */
  amountInCents: number;
  /** Moneda (COP). */
  currency: string;
  /** Llave pública del ambiente activo. */
  publicKey: string;
  /** Firma de integridad calculada en el servidor. */
  integritySignature: string;
  /** URL a la que Wompi devuelve al comprador. */
  redirectUrl: string;
  /** Ambiente de Wompi (test o prod). */
  environment: string;
  /** Nombre del comprador. */
  customerName: string;
  /** Correo del comprador. */
  customerEmail: string;
  /** Teléfono del comprador. */
  customerPhone: string;
  /** Subtotal calculado por el backend. */
  subtotal: number;
  /** Envío calculado por el backend. */
  shipping: number;
  /** Cargo fijo de servicio. */
  serviceFee: number;
  /** Total a cobrar. */
  total: number;
  /** Líneas con el precio definitivo del backend. */
  items: Order['items'];
}

/**
 * Estado de un pago consultado en la pantalla de resultado.
 *
 * @interface CheckoutStatus
 */
export interface CheckoutStatus {
  /** Referencia del pago. */
  reference: string;
  /** Pedido con su desglose. */
  order: Order;
  /** Estado del pago. */
  paymentStatus: PaymentStatus | null;
  /** Indica si el backend verificó el pago con Wompi. */
  verifiedPayment: boolean;
  /** Indica si se puede reintentar el pago. */
  canRetry: boolean;
  /** Mensaje listo para mostrar. */
  message: string;
  /** Momento de la consulta. */
  checkedAt: string;
}

/**
 * Transacción de Wompi vista por administración.
 *
 * @interface AdminPaymentTransaction
 */
export interface AdminPaymentTransaction {
  orderId: number;
  reference: string;
  wompiTransactionId?: string | null;
  paymentStatus?: string | null;
  orderStatus: string;
  paymentMethod?: string | null;
  environment?: string | null;
  customerName: string;
  customerEmail: string;
  subtotal: number;
  shipping: number;
  serviceFee: number;
  total: number;
  commissionAmount: number;
  createdAt: string;
  paidAt?: string | null;
  sellers: AdminPaymentSeller[];
}

/**
 * Distribución del dinero entre vendedores.
 *
 * @interface AdminPaymentSeller
 */
export interface AdminPaymentSeller {
  storeName?: string | null;
  sellerName?: string | null;
  subtotal: number;
  commission: number;
  net: number;
  fundsStatus?: string | null;
}

/**
 * Resumen de pagos del panel de administración.
 *
 * @interface AdminPayments
 */
export interface AdminPayments {
  totalTransactions: number;
  approvedCount: number;
  pendingCount: number;
  rejectedCount: number;
  serviceFeeRevenue: number;
  commissionRevenue: number;
  transactions: AdminPaymentTransaction[];
}
