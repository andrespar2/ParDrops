/**
 * @file src/app/core/services/cart.service.ts
 * @description Servicio para la gestiÃ³n del carrito de compras.
 * 
 * Este servicio maneja toda la lÃ³gica del carrito: aÃ±adir productos, eliminar,
 * actualizar cantidades, vaciar carrito y calcular totales. Utiliza signals
 * para un estado reactivo y gestiona la persistencia en localStorage.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, signal, computed, effect } from '@angular/core';
import { Cart, CartItem } from '../models/cart.model';
import { Product, ProductVariant } from '../models/product.model';
import {
  CURRENCY_CODE,
  FREE_SHIPPING_THRESHOLD,
  SHIPPING_COST,
  includedTax
} from '../config/commerce.config';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  /**
   * Clave para almacenar el carrito en localStorage.
   * @private
   * @constant
   */
  private readonly STORAGE_KEY = 'pardrops_cart';

  /**
   * Signal privado que almacena los Ã­tems del carrito.
   * @private
   */
  private readonly _items = signal<CartItem[]>(this.loadFromStorage());

  /**
   * Signal computado que expone todos los Ã­tems del carrito.
   * @returns {CartItem[]} Lista de Ã­tems en el carrito.
   */
  public readonly items = computed(() => this._items());

  /**
   * Signal computado que indica si el carrito estÃ¡ vacÃ­o.
   * @returns {boolean} True si no hay Ã­tems, false en caso contrario.
   */
  public readonly isEmpty = computed(() => this._items().length === 0);

  /**
   * Signal computado que cuenta el nÃºmero total de unidades en el carrito.
   * @returns {number} Total de unidades.
   */
  public readonly itemCount = computed(() => 
    this._items().reduce((total, item) => total + item.quantity, 0)
  );

  /**
   * Signal computado que calcula el subtotal del carrito.
   * @returns {number} Subtotal antes de impuestos y envÃ­o.
   */
  public readonly subtotal = computed(() => 
    this._items().reduce((total, item) => {
      const price = item.selectedVariant?.price ?? item.product.price;
      return total + (price * item.quantity);
    }, 0)
  );

  /**
   * Signal computado con los costes de envÃ­o (gratis desde el umbral de COP).
   * @returns {number} Coste de envÃ­o.
   */
  public readonly shipping = computed(() => {
    const groups = new Map<string, { subtotal: number; cost: number; threshold: number }>();

    for (const item of this._items()) {
      const key = item.product.storeSlug ?? item.product.sellerName ?? 'pardrops';
      const price = item.selectedVariant?.price ?? item.product.price;
      const group = groups.get(key) ?? {
        subtotal: 0,
        cost: item.product.shippingCost ?? SHIPPING_COST,
        threshold: item.product.freeShippingThreshold ?? FREE_SHIPPING_THRESHOLD
      };
      group.subtotal += price * item.quantity;
      groups.set(key, group);
    }

    let total = 0;
    for (const group of groups.values()) {
      total += group.subtotal >= group.threshold ? 0 : group.cost;
    }
    return Math.round(total);
  });

  /**
   * IVA incluido en los precios (informativo). No se suma al total.
   * @returns {number} Porción de impuesto incluida en el subtotal.
   */
  public readonly tax = computed(() => includedTax(this.subtotal()));

  /**
   * Signal computado con el total final.
   * @returns {number} Total a pagar.
   */
  public readonly total = computed(() =>
    Math.round(this.subtotal() + this.shipping())
  );

  /** Subtotal a partir del cual el envÃ­o es gratuito. */
  public readonly freeShippingThreshold = FREE_SHIPPING_THRESHOLD;

  /**
   * Signal computado que devuelve el estado completo del carrito.
   * @returns {Cart} Estado completo del carrito.
   */
  public readonly cart = computed<Cart>(() => ({
    items: this._items(),
    subtotal: this.subtotal(),
    tax: this.tax(),
    shipping: this.shipping(),
    total: this.total(),
    currency: CURRENCY_CODE,
    itemCount: this.itemCount(),
    isEmpty: this.isEmpty()
  }));

  constructor() {
    // Persistir el carrito en localStorage cada vez que cambia
    effect(() => {
      const items = this._items();
      this.saveToStorage(items);
    });
  }

  /**
   * AÃ±ade un producto al carrito.
   * Si el producto ya existe (misma variante), incrementa la cantidad.
   * 
   * @param {Product} product - Producto a aÃ±adir.
   * @param {number} [quantity=1] - Cantidad a aÃ±adir.
   * @param {ProductVariant} [selectedVariant] - Variante seleccionada.
   * @returns {void}
   */
  addItem(product: Product, quantity: number = 1, selectedVariant?: ProductVariant): void {
    const existingItemIndex = this._items().findIndex(item => 
      item.product.id === product.id && 
      this.areVariantsEqual(item.selectedVariant, selectedVariant)
    );

    if (existingItemIndex > -1) {
      // Actualizar cantidad del Ã­tem existente
      const updatedItems = [...this._items()];
      const newQuantity = updatedItems[existingItemIndex].quantity + quantity;
      // Verificar stock disponible
      const maxStock = selectedVariant?.stock ?? product.stock;
      updatedItems[existingItemIndex].quantity = Math.min(newQuantity, maxStock);
      this._items.set(updatedItems);
    } else {
      // Crear nuevo Ã­tem
      const maxStock = selectedVariant?.stock ?? product.stock;
      const finalQuantity = Math.min(quantity, maxStock);
      
      if (finalQuantity <= 0) return;

      const newItem: CartItem = {
        id: `${product.id}-${selectedVariant?.id ?? 'default'}-${Date.now()}`,
        product,
        quantity: finalQuantity,
        selectedVariant,
        subtotal: (selectedVariant?.price ?? product.price) * finalQuantity
      };
      
      this._items.set([...this._items(), newItem]);
    }
  }

  /**
   * Actualiza la cantidad de un Ã­tem especÃ­fico del carrito.
   * 
   * @param {string} itemId - ID del Ã­tem a actualizar.
   * @param {number} quantity - Nueva cantidad.
   * @returns {void}
   */
  updateQuantity(itemId: string, quantity: number): void {
    if (quantity <= 0) {
      this.removeItem(itemId);
      return;
    }

    const updatedItems = this._items().map(item => {
      if (item.id === itemId) {
        const maxStock = item.selectedVariant?.stock ?? item.product.stock;
        const finalQuantity = Math.min(quantity, maxStock);
        const price = item.selectedVariant?.price ?? item.product.price;
        return {
          ...item,
          quantity: finalQuantity,
          subtotal: price * finalQuantity
        };
      }
      return item;
    });

    this._items.set(updatedItems);
  }

  /**
   * Elimina un Ã­tem del carrito.
   * 
   * @param {string} itemId - ID del Ã­tem a eliminar.
   * @returns {void}
   */
  removeItem(itemId: string): void {
    this._items.set(this._items().filter(item => item.id !== itemId));
  }

  /**
   * Sincroniza los precios del carrito con los confirmados por el backend.
   *
   * @param items líneas del pedido con el precio definitivo.
   * @returns {void}
   */
  public syncPrices(items: Array<{ productId: string; variantName?: string | null; unitPrice: number }>): void {
    if (items.length === 0) {
      return;
    }

    const prices = new Map<string, number>();
    for (const item of items) {
      prices.set(`${item.productId}|${item.variantName ?? ''}`, item.unitPrice);
    }

    const updated = this._items().map(cartItem => {
      const key = `${cartItem.product.id}|${cartItem.selectedVariant?.name ?? ''}`;
      const price = prices.get(key);
      if (price === undefined) {
        return cartItem;
      }

      const currentPrice = cartItem.selectedVariant?.price ?? cartItem.product.price;
      if (Math.abs(price - currentPrice) < 0.01) {
        return cartItem;
      }

      const product = cartItem.selectedVariant
        ? cartItem.product
        : { ...cartItem.product, price };
      const selectedVariant = cartItem.selectedVariant
        ? { ...cartItem.selectedVariant, price }
        : undefined;

      return {
        ...cartItem,
        product,
        selectedVariant,
        subtotal: price * cartItem.quantity
      };
    });

    this._items.set(updated);
  }

  /**
   * Incrementa la cantidad de un Ã­tem en 1 unidad.
   * 
   * @param {string} itemId - ID del Ã­tem.
   * @returns {void}
   */
  increaseQuantity(itemId: string): void {
    const item = this._items().find(i => i.id === itemId);
    if (item) {
      this.updateQuantity(itemId, item.quantity + 1);
    }
  }

  /**
   * Decrementa la cantidad de un Ã­tem en 1 unidad.
   * 
   * @param {string} itemId - ID del Ã­tem.
   * @returns {void}
   */
  decreaseQuantity(itemId: string): void {
    const item = this._items().find(i => i.id === itemId);
    if (item) {
      this.updateQuantity(itemId, item.quantity - 1);
    }
  }

  /**
   * VacÃ­a completamente el carrito de compras.
   * 
   * @returns {void}
   */
  clearCart(): void {
    this._items.set([]);
  }

  /**
   * Verifica si dos variantes son iguales.
   * 
   * @param {ProductVariant | undefined} variant1 - Primera variante.
   * @param {ProductVariant | undefined} variant2 - Segunda variante.
   * @returns {boolean} True si son iguales, false en caso contrario.
   * @private
   */
  private areVariantsEqual(
    variant1?: ProductVariant,
    variant2?: ProductVariant
  ): boolean {
    if (!variant1 && !variant2) return true;
    if (!variant1 || !variant2) return false;
    return variant1.id === variant2.id;
  }

  /**
   * Carga el carrito desde localStorage.
   * 
   * @returns {CartItem[]} Lista de Ã­tems almacenados.
   * @private
   */
  private loadFromStorage(): CartItem[] {
    try {
      if (typeof window === 'undefined') return [];
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (!stored) return [];
      return JSON.parse(stored);
    } catch (error) {
      console.error('Error al cargar el carrito desde localStorage:', error);
      return [];
    }
  }

  /**
   * Guarda el carrito en localStorage.
   * 
   * @param {CartItem[]} items - Ãtems a guardar.
   * @returns {void}
   * @private
   */
  private saveToStorage(items: CartItem[]): void {
    try {
      if (typeof window === 'undefined') return;
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(items));
    } catch (error) {
      console.error('Error al guardar el carrito en localStorage:', error);
    }
  }
}
