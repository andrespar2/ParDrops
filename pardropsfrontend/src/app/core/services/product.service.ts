/**
 * @file src/app/core/services/product.service.ts
 * @description Servicio para la gestión y obtención de productos.
 *
 * Carga el catálogo desde la API REST del backend y expone signals reactivos
 * para que las vistas se actualicen solas. También incluye las operaciones de
 * creación, edición y borrado usadas por el panel de administración, que
 * mantienen la lista local sincronizada tras cada cambio.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Product } from '../models/product.model';
import { ProductRequest } from '../models/product-request.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** URL base de los endpoints de productos. */
  private readonly productsUrl = `${API_BASE_URL}/products`;

  /** Signal privado con la lista de productos cargada desde la API. */
  private readonly _products = signal<Product[]>([]);

  /** Signal que indica si hay una carga en curso. */
  private readonly _loading = signal<boolean>(false);

  /** Signal que indica si el catálogo ya se ha cargado al menos una vez. */
  private readonly _loaded = signal<boolean>(false);

  /** Signal con el último mensaje de error de carga (si lo hubo). */
  private readonly _error = signal<string | null>(null);

  /** Lista completa de productos (solo lectura). */
  public readonly products = this._products.asReadonly();

  /** Indica si el catálogo está cargándose. */
  public readonly loading = this._loading.asReadonly();

  /** Indica si el catálogo ya se cargó. */
  public readonly loaded = this._loaded.asReadonly();

  /** Último error de carga, si existe. */
  public readonly error = this._error.asReadonly();

  /** Productos destacados. */
  public readonly featuredProducts = computed(() =>
    this._products().filter(product => product.isFeatured)
  );

  /** Productos marcados como novedad. */
  public readonly newProducts = computed(() =>
    this._products().filter(product => product.isNew)
  );

  /** Productos en oferta. */
  public readonly saleProducts = computed(() =>
    this._products().filter(product => product.onSale)
  );

  constructor() {
    this.loadProducts();
  }

  /**
   * Carga (o recarga) el catálogo completo desde la API.
   *
   * @returns {void}
   */
  public loadProducts(): void {
    this._loading.set(true);
    this._error.set(null);

    this.http.get<Product[]>(this.productsUrl).subscribe({
      next: products => {
        this._products.set(products);
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
   * Busca un producto por su slug en la lista ya cargada.
   *
   * @param slug slug del producto.
   * @returns el producto encontrado o `undefined`.
   */
  public getProductBySlug(slug: string): Product | undefined {
    return this._products().find(product => product.slug === slug);
  }

  /**
   * Obtiene todos los productos de una categoría.
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
   * Busca productos por término de búsqueda.
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
      tap(created => this._products.update(list => [...list, created]))
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
      tap(updated => this._products.update(list =>
        list.map(product => (product.id === id ? updated : product))
      ))
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
      tap(() => this._products.update(list => list.filter(product => product.id !== id)))
    );
  }
}
