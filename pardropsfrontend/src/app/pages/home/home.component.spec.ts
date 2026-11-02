import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { describe, expect, it, beforeEach } from 'vitest';
import { HomeComponent } from './home.component';
import { ProductService } from '../../core/services/product.service';
import { AuthService } from '../../core/services/auth.service';
import { Product } from '../../core/models/product.model';

/** Crea un producto de prueba. */
function product(id: string, price: number, category: string, isNew = false): Product {
  return {
    id,
    name: `Producto ${id}`,
    slug: `producto-${id}`,
    description: 'DescripciÃ³n de prueba',
    price,
    currency: 'COP',
    stock: 5,
    categories: [category],
    tags: [],
    images: [],
    variants: [],
    isNew,
    onSale: false,
    isFeatured: false,
    active: true,
    sellerId: 1,
    sellerName: 'Tienda de prueba',
    storeSlug: 'tienda-de-prueba',
    createdAt: new Date('2026-01-01T00:00:00Z'),
    updatedAt: new Date('2026-01-01T00:00:00Z')
  };
}

describe('HomeComponent', () => {
  let products: Product[];

  beforeEach(() => {
    products = Array.from({ length: 20 }, (_, index) =>
      product(`p${index}`, 10_000 * (index + 1), index % 2 === 0 ? 'Ropa' : 'Calzado', index < 3),
    );

    const productService = {
      products: signal(products),
      loading: signal(false),
      error: signal<string | null>(null),
      loadProducts: () => undefined,
    };

    TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: AuthService, useValue: { isSeller: signal(false) } },
        { provide: ActivatedRoute, useValue: { queryParamMap: of(convertToParamMap({})) } },
      ],
    });
  });

  it('muestra como mÃ¡ximo 12 productos y permite cargar mÃ¡s', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    const component = fixture.componentInstance;

    expect(component.visible().length).toBe(12);
    expect(component.hasMore()).toBe(true);

    component.loadMore();

    expect(component.visible().length).toBe(20);
    expect(component.hasMore()).toBe(false);
  });

  it('filtra por categorÃ­a y por precio', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    const component = fixture.componentInstance;

    component.selectCategory('Ropa');
    expect(component.filtered().every(item => item.categories.includes('Ropa'))).toBe(true);

    component.selectCategory('all');
    component.selectRange('lt100');
    expect(component.filtered().every(item => item.price < 100_000)).toBe(true);
  });

  it('ordena por precio ascendente y descendente', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    const component = fixture.componentInstance;

    component.selectSort('price-asc');
    const ascending = component.filtered().map(item => item.price);
    expect([...ascending].sort((a, b) => a - b)).toEqual(ascending);

    component.selectSort('price-desc');
    const descending = component.filtered().map(item => item.price);
    expect([...descending].sort((a, b) => b - a)).toEqual(descending);
  });

  it('limita la carga inicial a 12 aunque haya mÃ¡s resultados', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    const component = fixture.componentInstance;

    component.selectCategory('Calzado');
    expect(component.filtered().length).toBe(10);
    expect(component.visible().length).toBe(10);
  });
});

