/**
 * @file src/app/core/models/cart.model.ts
 * @description Define las interfaces y tipos relacionados con el carrito de compras.
 * 
 * Este archivo contiene las estructuras de datos para representar los items
 * en el carrito, el estado completo del carrito y las operaciones relacionadas
 * con la gestión de productos añadidos.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Product, ProductVariant } from './product.model';

/**
 * Interfaz que representa un ítem dentro del carrito de compras.
 * 
 * Cada ítem contiene el producto, la cantidad seleccionada, la variante
 * elegida (si aplica) y el subtotal calculado.
 * 
 * @interface CartItem
 * @property {string} id - Identificador único del ítem en el carrito.
 * @property {Product} product - Producto asociado al ítem.
 * @property {number} quantity - Cantidad de productos en este ítem.
 * @property {ProductVariant} [selectedVariant] - Variante seleccionada del producto.
 * @property {number} [subtotal] - Subtotal calculado (precio * cantidad).
 */
export interface CartItem {
  /** Identificador único del ítem del carrito */
  id: string;
  /** Producto asociado */
  product: Product;
  /** Cantidad seleccionada */
  quantity: number;
  /** Variante seleccionada (opcional) */
  selectedVariant?: ProductVariant;
  /** Subtotal del ítem (opcional, calculado) */
  subtotal?: number;
}

/**
 * Interfaz que representa el estado completo del carrito de compras.
 * 
 * Contiene todos los ítems, los totales calculados y la información
 * relacionada con el proceso de compra.
 * 
 * @interface Cart
 * @property {CartItem[]} items - Array de ítems en el carrito.
 * @property {number} subtotal - Subtotal total (suma de subtotales de ítems, en COP).
 * @property {number} tax - Impuestos aplicables (IVA 19 %, en COP).
 * @property {number} shipping - Costes de envío (en COP; gratis desde 400.000).
 * @property {number} total - Total final (subtotal + tax + shipping, en COP).
 * @property {string} currency - Moneda de los totales (por defecto 'COP').
 * @property {number} itemCount - Número total de unidades.
 * @property {boolean} isEmpty - Indica si el carrito está vacío.
 */
export interface Cart {
  /** Lista de ítems en el carrito */
  items: CartItem[];
  /** Subtotal antes de impuestos y envío (COP) */
  subtotal: number;
  /** Impuestos aplicados (IVA 19 %, COP) */
  tax: number;
  /** Coste de envío (COP; gratis a partir de 400.000) */
  shipping: number;
  /** Total a pagar (COP) */
  total: number;
  /** Moneda (siempre 'COP': la plataforma opera en Colombia) */
  currency: string;
  /** Número total de ítems (unidades) */
  itemCount: number;
  /** Indica si el carrito está vacío */
  isEmpty: boolean;
}