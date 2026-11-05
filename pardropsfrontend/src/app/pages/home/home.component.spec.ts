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
    description: 'Descripción de prueba',
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
  let loadProductsCalls: Array<Record<string, unknown>>;
  let loadMoreCalls: number;

  beforeEach(() => {
    products = Array.from({ length: 20 }, (_, index) =>
      product(`p${index}`, 10_000 * (index + 1), index % 2 === 0 ? 'Ropa' : 'Calzado', index < 3),
    );
    loadProductsCalls = [];
    loadMoreCalls = 0;

    const productService = {
      products: signal(products),
      loading: signal(false),
      error: signal<string | null>(null),
      totalElements: signal(products.length),
      hasMore: signal(true),
      loadProducts: (filters: Record<string, unknown>) => {
        loadProductsCalls.push(filters ?? {});
      },
      loadMore: () => {
        loadMoreCalls++;
      },
      reloadDistribution: () => undefined
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

  it('muestra los productos de la página y delega la carga de más', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    const component = fixture.componentInstance;

    expect(component.visible().length).toBe(20);
    expect(component.hasMore()).toBe(true);

    component.loadMore();

    expect(loadMoreCalls).toBe(1);
  });

  it('envía la categoría y el rango de precio al servidor', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    const component = fixture.componentInstance;

    component.selectCategory('Ropa');
    expect(loadProductsCalls.at(-1)?.['category']).toBe('Ropa');

    component.selectRange('lt100');
    expect(loadProductsCalls.at(-1)?.['maxPrice']).toBe(99_999);
  });

  it('envía el orden seleccionado al servidor', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    const component = fixture.componentInstance;

    component.selectSort('price-asc');
    expect(loadProductsCalls.at(-1)?.['sort']).toBe('price-asc');

    component.selectSort('price-desc');
    expect(loadProductsCalls.at(-1)?.['sort']).toBe('price-desc');
  });

  it('usa el total del servidor para los filtros rápidos', () => {
    const fixture = TestBed.createComponent(HomeComponent);
    const component = fixture.componentInstance;

    component.clearFilters();

    expect(loadProductsCalls.at(-1)?.['onlyNew']).toBeUndefined();
    expect(component.totalElements()).toBe(20);
  });
});
