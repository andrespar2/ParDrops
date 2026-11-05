/**
 * @file src/app/core/services/product.service.ts
 * @description Servicio del catálogo de productos.
 *
 * El catálogo se carga por páginas con una semilla de distribución que se
 * conserva durante la navegación. Cada recarga completa de la aplicación crea
 * una semilla nueva, por lo que el orden cambia sin romper la paginación.
 * Las respuestas públicas se guardan en la caché local acotada del navegador.
 */

import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  Product,
  ProductFeedFilters,
  ProductFeedPage,
  ProductHighlights
} from '../models/product.model';
import { ProductRequest } from '../models/product-request.model';
import { LocalCacheService } from './local-cache.service';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Caché local del dispositivo. */
  private readonly localCache = inject(LocalCacheService);

  /** URL base de los endpoints de productos. */
  private readonly productsUrl = `${API_BASE_URL}/products`;

  /** Tamaño de página del catálogo. */
  private readonly pageSize = 24;

  /** Semilla de la distribución actual (en memoria: cambia al recargar). */
  private readonly seed = this.newSeed();

  /** Filtros aplicados en la última carga. */
  private filters: ProductFeedFilters = {};

  /** Signal privado con los productos cargados. */
  private readonly _products = signal<Product[]>([]);

  /** Signal que indica si hay una carga en curso. */
  private readonly _loading = signal<boolean>(false);

  /** Signal que indica si el catálogo ya se ha cargado al menos una vez. */
  private readonly _loaded = signal<boolean>(false);

  /** Signal con el último mensaje de error de carga. */
  private readonly _error = signal<string | null>(null);

  /** Total de productos que cumplen los filtros. */
  private readonly _totalElements = signal<number>(0);

  /** Total de páginas disponibles. */
  private readonly _totalPages = signal<number>(0);

  /** Página actual (0-based). */
  private readonly _currentPage = signal<number>(0);

  /** Lista de productos visibles. */
  public readonly products = this._products.asReadonly();

  /** Indica si el catálogo está cargándose. */
  public readonly loading = this._loading.asReadonly();

  /** Indica si el catálogo ya se cargó. */
  public readonly loaded = this._loaded.asReadonly();

  /** Último error de carga, si existe. */
  public readonly error = this._error.asReadonly();

  /** Total de productos del filtro actual. */
  public readonly totalElements = this._totalElements.asReadonly();

  /** Total de páginas del filtro actual. */
  public readonly totalPages = this._totalPages.asReadonly();

  /** Página actual. */
  public readonly currentPage = this._currentPage.asReadonly();

  /** Indica si quedan páginas por cargar. */
  public readonly hasMore = computed(
    () => this._currentPage() + 1 < this._totalPages()
  );

  /** Productos destacados de la página cargada. */
  public readonly featuredProducts = computed(() =>
    this._products().filter(product => product.isFeatured)
  );

  /** Productos marcados como novedad en la página cargada. */
  public readonly newProducts = computed(() =>
    this._products().filter(product => product.isNew)
  );

  /** Productos en oferta de la página cargada. */
  public readonly saleProducts = computed(() =>
    this._products().filter(product => product.onSale)
  );

  constructor() {
    this.loadProducts();
  }

  /**
   * Carga la primera página con los filtros indicados.
   *
   * @param filters filtros del catálogo.
   */
  public loadProducts(filters: ProductFeedFilters = {}): void {
    this.filters = { ...filters };
    this._currentPage.set(0);
    this._loading.set(true);
    this._error.set(null);

    this.requestPage(0).subscribe({
      next: page => {
        this._products.set(page.content);
        this._totalElements.set(page.totalElements);
        this._totalPages.set(page.totalPages);
        this._currentPage.set(page.page);
        this._loaded.set(true);
        this._loading.set(false);
      },
      error: () => {
        this._error.set('No se pudo cargar el catálogo de productos.');
        this._loading.set(false);
      }
    });
  }

  /**
   * Carga la siguiente página y la añade a la lista actual.
   */
  public loadMore(): void {
    if (this._loading() || !this.hasMore()) {
      return;
    }
    const nextPage = this._currentPage() + 1;
    this._loading.set(true);

    this.requestPage(nextPage).subscribe({
      next: page => {
        this._products.update(current => {
          const seen = new Set(current.map(product => product.id));
          const added = page.content.filter(product => !seen.has(product.id));
          return [...current, ...added];
        });
        this._currentPage.set(page.page);
        this._totalPages.set(page.totalPages);
        this._totalElements.set(page.totalElements);
        this._loading.set(false);
      },
      error: () => {
        this._error.set('No se pudo cargar más productos.');
        this._loading.set(false);
      }
    });
  }

  /**
   * Fuerza una nueva distribución y vuelve a la primera página.
   *
   * @param filters filtros opcionales.
   */
  public reloadDistribution(filters: ProductFeedFilters = this.filters): void {
    this.localCache.clear();
    this.loadProducts(filters);
  }

  /**
   * Busca un producto por su slug en la lista ya cargada.
   *
   * @param slug slug del producto.
   * @returns el producto encontrado o `undefined`.
   */
  public getProductBySlug(slug: string): Product | undefined {
    return this._products().find(product => product.slug === slug);
  }

  /**
   * Obtiene un producto del servidor (con caché local breve).
   *
   * @param slug slug del producto.
   * @returns observable con el producto.
   */
  public fetchProductBySlug(slug: string): Observable<Product> {
    const cacheKey = `product:${slug}`;
    const cached = this.localCache.get<Product>(cacheKey);
    if (cached) {
      return of(cached);
    }
    return this.http.get<Product>(`${this.productsUrl}/${encodeURIComponent(slug)}`).pipe(
      tap(product => this.localCache.set(cacheKey, product, 60))
    );
  }

  /**
   * Invalida la ficha local de un producto tras una modificación.
   *
   * @param slug slug del producto.
   */
  public invalidateProduct(slug: string): void {
    this.localCache.remove(`product:${slug}`);
    this.localCache.remove('highlights');
  }

  /**
   * Invalida toda la caché pública local tras una escritura del vendedor.
   */
  public clearLocalCache(): void {
    this.localCache.clear();
  }

  /**
   * Resuelve una lista de productos por identificador (favoritos).
   *
   * @param ids identificadores guardados.
   * @returns observable con los productos publicados.
   */
  public fetchProductsByIds(ids: string[]): Observable<Product[]> {
    if (ids.length === 0) {
      return of([]);
    }
    const params = new HttpParams().set('ids', ids.join(','));
    return this.http.get<Product[]>(`${this.productsUrl}/by-ids`, { params });
  }

  /**
   * Obtiene los rankings de la portada con caché local de cinco minutos.
   *
   * @returns observable con los más vendidos y los mejor valorados.
   */
  public getHighlights(): Observable<ProductHighlights> {
    const cacheKey = 'highlights';
    const cached = this.localCache.get<ProductHighlights>(cacheKey);
    if (cached) {
      return of(cached);
    }
    return this.http.get<ProductHighlights>(`${this.productsUrl}/highlights`).pipe(
      tap(highlights => this.localCache.set(cacheKey, highlights, 300))
    );
  }

  /**
   * Obtiene todos los productos de una categoría de la página actual.
   *
   * @param category nombre de la categoría.
   * @returns productos filtrados.
   */
  public getProductsByCategory(category: string): Product[] {
    return this._products().filter(product =>
      product.categories.some(cat => cat.toLowerCase() === category.toLowerCase())
    );
  }

  /**
   * Busca productos en la página actual.
   *
   * @param query término a buscar.
   * @returns productos que coinciden.
   */
  public searchProducts(query: string): Product[] {
    const lowerQuery = query.toLowerCase().trim();
    if (!lowerQuery) return this._products();

    return this._products().filter(product =>
      product.name.toLowerCase().includes(lowerQuery) ||
      product.description.toLowerCase().includes(lowerQuery) ||
      product.tags?.some(tag => tag.toLowerCase().includes(lowerQuery)) ||
      product.categories.some(cat => cat.toLowerCase().includes(lowerQuery))
    );
  }

  // ------------------------------------------------------------------
  // Operaciones de administración (crear, editar y eliminar).
  // ------------------------------------------------------------------

  /**
   * Crea un producto y lo añade a la lista local.
   *
   * @param request datos del nuevo producto.
   * @returns observable con el producto creado.
   */
  public createProduct(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.productsUrl, request).pipe(
      tap(created => {
        this.localCache.clear();
        this._products.update(list => [...list, created]);
      })
    );
  }

  /**
   * Actualiza un producto y refresca la copia local.
   *
   * @param id      identificador del producto.
   * @param request datos actualizados.
   * @returns observable con el producto actualizado.
   */
  public updateProduct(id: string, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.productsUrl}/${id}`, request).pipe(
      tap(updated => {
        this.localCache.clear();
        this._products.update(list =>
          list.map(product => (product.id === id ? updated : product))
        );
      })
    );
  }

  /**
   * Elimina un producto y lo quita de la lista local.
   *
   * @param id identificador del producto.
   * @returns observable que completa al borrar.
   */
  public deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.productsUrl}/${id}`).pipe(
      tap(() => {
        this.localCache.clear();
        this._products.update(list => list.filter(product => product.id !== id));
      })
    );
  }

  private requestPage(page: number): Observable<ProductFeedPage> {
    let params = new HttpParams()
      .set('seed', this.seed)
      .set('page', String(page))
      .set('size', String(this.pageSize));

    if (this.filters.search?.trim()) params = params.set('search', this.filters.search.trim());
    if (this.filters.category) params = params.set('category', this.filters.category);
    if (this.filters.minPrice != null) params = params.set('minPrice', String(this.filters.minPrice));
    if (this.filters.maxPrice != null) params = params.set('maxPrice', String(this.filters.maxPrice));
    if (this.filters.onlyNew) params = params.set('new', 'true');
    if (this.filters.onlySale) params = params.set('sale', 'true');
    if (this.filters.sort) params = params.set('sort', this.filters.sort);

    return this.http.get<ProductFeedPage>(`${this.productsUrl}/feed`, { params });
  }

  private newSeed(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }
}
