import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { CartService } from './cart.service';
import { Product } from '../models/product.model';

/**
 * Crea un producto de prueba con su configuración de envío.
 */
function product(
  id: string,
  price: number,
  requiresShipping: boolean,
  shippingCost: number,
  threshold: number | null
): Product {
  return {
    id,
    name: `Producto ${id}`,
    slug: `producto-${id}`,
    description: 'Producto de prueba',
    price,
    currency: 'COP',
    images: [],
    categories: ['Ropa'],
    stock: 10,
    isNew: false,
    isFeatured: false,
    onSale: false,
    requiresShipping,
    shippingCost,
    freeShippingThreshold: threshold
  };
}

describe('CartService · envío por producto', () => {
  let cart: CartService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    cart = TestBed.inject(CartService);
    cart.clearCart();
  });

  it('suma el envío de cada producto que lo requiere', () => {
    cart.addItem(product('a', 10_000, true, 5_000, null), 1);
    cart.addItem(product('b', 10_000, true, 8_000, null), 1);

    expect(cart.shipping()).toBe(13_000);
  });

  it('no cobra envío de un producto que no lo requiere', () => {
    cart.addItem(product('a', 10_000, false, 25_900, 400_000), 1);

    expect(cart.shipping()).toBe(0);
    expect(cart.total()).toBe(10_000);
  });

  it('aplica envío gratis al producto que alcanza su umbral', () => {
    cart.addItem(product('a', 60_000, true, 5_000, 50_000), 1);
    cart.addItem(product('b', 10_000, true, 8_000, 100_000), 1);

    expect(cart.shipping()).toBe(8_000);
  });

  it('el total es subtotal más envío', () => {
    cart.addItem(product('a', 100_000, true, 5_000, 500_000), 1);

    expect(cart.subtotal()).toBe(100_000);
    expect(cart.shipping()).toBe(5_000);
    expect(cart.total()).toBe(105_000);
  });

  it('sincroniza los precios confirmados por el backend', () => {
    cart.addItem(product('a', 100_000, true, 5_000, 500_000), 1);

    cart.syncPrices([{ productId: 'a', variantName: null, unitPrice: 120_000 }]);

    expect(cart.items()[0].product.price).toBe(120_000);
    expect(cart.subtotal()).toBe(120_000);
  });
});
