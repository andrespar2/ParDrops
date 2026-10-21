/**
 * @file src/app/core/services/cart.service.ts
 * @description Servicio para la gestión del carrito de compras.
 * 
 * Este servicio maneja toda la lógica del carrito: añadir productos, eliminar,
 * actualizar cantidades, vaciar carrito y calcular totales. Utiliza signals
 * para un estado reactivo y gestiona la persistencia en localStorage.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, signal, computed, effect } from '@angular/core';
import { Cart, CartItem } from '../models/cart.model';
import { Product, ProductVariant } from '../models/product.model';

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
   * Signal privado que almacena los ítems del carrito.
   * @private
   */
  private readonly _items = signal<CartItem[]>(this.loadFromStorage());

  /**
   * Signal computado que expone todos los ítems del carrito.
   * @returns {CartItem[]} Lista de ítems en el carrito.
   */
  public readonly items = computed(() => this._items());

  /**
   * Signal computado que indica si el carrito está vacío.
   * @returns {boolean} True si no hay ítems, false en caso contrario.
   */
  public readonly isEmpty = computed(() => this._items().length === 0);

  /**
   * Signal computado que cuenta el número total de unidades en el carrito.
   * @returns {number} Total de unidades.
   */
  public readonly itemCount = computed(() => 
    this._items().reduce((total, item) => total + item.quantity, 0)
  );

  /**
   * Signal computado que calcula el subtotal del carrito.
   * @returns {number} Subtotal antes de impuestos y envío.
   */
  public readonly subtotal = computed(() => 
    this._items().reduce((total, item) => {
      const price = item.selectedVariant?.price ?? item.product.price;
      return total + (price * item.quantity);
    }, 0)
  );

  /**
   * Signal computado con los costes de envío (gratis a partir de 400.000 COP).
   * @returns {number} Coste de envío.
   */
  public readonly shipping = computed(() => {
    const subtotal = this.subtotal();
    return subtotal >= 400000 ? 0 : 25900;
  });

  /**
   * Signal computado con los impuestos (IVA 19%).
   * @returns {number} Importe de impuestos.
   */
  public readonly tax = computed(() => this.subtotal() * 0.19);

  /**
   * Signal computado con el total final.
   * @returns {number} Total a pagar.
   */
  public readonly total = computed(() => 
    this.subtotal() + this.tax() + this.shipping()
  );

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
    currency: 'COP',
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
   * Añade un producto al carrito.
   * Si el producto ya existe (misma variante), incrementa la cantidad.
   * 
   * @param {Product} product - Producto a añadir.
   * @param {number} [quantity=1] - Cantidad a añadir.
   * @param {ProductVariant} [selectedVariant] - Variante seleccionada.
   * @returns {void}
   */
  addItem(product: Product, quantity: number = 1, selectedVariant?: ProductVariant): void {
    const existingItemIndex = this._items().findIndex(item => 
      item.product.id === product.id && 
      this.areVariantsEqual(item.selectedVariant, selectedVariant)
    );

    if (existingItemIndex > -1) {
      // Actualizar cantidad del ítem existente
      const updatedItems = [...this._items()];
      const newQuantity = updatedItems[existingItemIndex].quantity + quantity;
      // Verificar stock disponible
      const maxStock = selectedVariant?.stock ?? product.stock;
      updatedItems[existingItemIndex].quantity = Math.min(newQuantity, maxStock);
      this._items.set(updatedItems);
    } else {
      // Crear nuevo ítem
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
   * Actualiza la cantidad de un ítem específico del carrito.
   * 
   * @param {string} itemId - ID del ítem a actualizar.
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
   * Elimina un ítem del carrito.
   * 
   * @param {string} itemId - ID del ítem a eliminar.
   * @returns {void}
   */
  removeItem(itemId: string): void {
    this._items.set(this._items().filter(item => item.id !== itemId));
  }

  /**
   * Incrementa la cantidad de un ítem en 1 unidad.
   * 
   * @param {string} itemId - ID del ítem.
   * @returns {void}
   */
  increaseQuantity(itemId: string): void {
    const item = this._items().find(i => i.id === itemId);
    if (item) {
      this.updateQuantity(itemId, item.quantity + 1);
    }
  }

  /**
   * Decrementa la cantidad de un ítem en 1 unidad.
   * 
   * @param {string} itemId - ID del ítem.
   * @returns {void}
   */
  decreaseQuantity(itemId: string): void {
    const item = this._items().find(i => i.id === itemId);
    if (item) {
      this.updateQuantity(itemId, item.quantity - 1);
    }
  }

  /**
   * Vacía completamente el carrito de compras.
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
   * @returns {CartItem[]} Lista de ítems almacenados.
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
   * @param {CartItem[]} items - Ítems a guardar.
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