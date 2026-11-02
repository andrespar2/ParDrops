import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { CartService } from './cart.service';
import { Product } from '../models/product.model';

/**
 * Crea un producto de prueba con la configuración de envío indicada.
 */
function product(id: string, price: number, storeSlug: string, shippingCost: number, threshold: number): Product {
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
    storeSlug,
    shippingCost,
    freeShippingThreshold: threshold
  };
}

describe('CartService · envío por tienda', () => {
  let cart: CartService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    cart = TestBed.inject(CartService);
    cart.clearCart();
  });

  it('suma el envío de cada tienda por separado', () => {
    cart.addItem(product('a', 10_000, 'tienda-a', 5_000, 100_000), 1);
    cart.addItem(product('b', 10_000, 'tienda-b', 8_000, 100_000), 1);

    expect(cart.shipping()).toBe(13_000);
  });

  it('aplica el envío gratis solo a la tienda que alcanza su umbral', () => {
    cart.addItem(product('a', 60_000, 'tienda-a', 5_000, 50_000), 1);
    cart.addItem(product('b', 10_000, 'tienda-b', 8_000, 100_000), 1);

    expect(cart.shipping()).toBe(8_000);
  });

  it('no cobra envío cuando las dos tiendas superan su umbral', () => {
    cart.addItem(product('a', 80_000, 'tienda-a', 5_000, 50_000), 1);
    cart.addItem(product('b', 90_000, 'tienda-b', 8_000, 50_000), 1);

    expect(cart.shipping()).toBe(0);
  });

  it('el total es subtotal más envío, sin recargo de IVA', () => {
    cart.addItem(product('a', 100_000, 'tienda-a', 5_000, 500_000), 1);

    expect(cart.subtotal()).toBe(100_000);
    expect(cart.shipping()).toBe(5_000);
    expect(cart.total()).toBe(105_000);
    expect(cart.tax()).toBe(15_966);
  });
});
