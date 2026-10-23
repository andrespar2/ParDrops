/**
 * @file src/app/core/services/store.service.ts
 * @description Servicio de las tiendas públicas y de la ficha propia del
 * vendedor.
 *
 * Las lecturas son públicas (cualquier visitante puede ver una tienda y su
 * catálogo). La edición de la ficha requiere sesión de vendedor y la valida el
 * backend.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Product } from '../models/product.model';
import { Store, StoreRequest } from '../models/store.model';

@Injectable({
  providedIn: 'root'
})
export class StoreService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** URL base de los endpoints de tiendas. */
  private readonly storesUrl = `${API_BASE_URL}/stores`;

  /**
   * Lista las tiendas públicas.
   *
   * @returns observable con las tiendas.
   */
  public list(): Observable<Store[]> {
    return this.http.get<Store[]>(this.storesUrl);
  }

  /**
   * Obtiene la ficha pública de una tienda por su slug.
   *
   * @param slug slug de la tienda.
   * @returns observable con la tienda.
   */
  public getBySlug(slug: string): Observable<Store> {
    return this.http.get<Store>(`${this.storesUrl}/${slug}`);
  }

  /**
   * Obtiene los productos publicados de una tienda.
   *
   * @param slug slug de la tienda.
   * @returns observable con los productos.
   */
  public getProducts(slug: string): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.storesUrl}/${slug}/products`);
  }

  /**
   * Obtiene la tienda del vendedor autenticado.
   *
   * @returns observable con su ficha comercial.
   */
  public getMine(): Observable<Store> {
    return this.http.get<Store>(`${this.storesUrl}/mine`);
  }

  /**
   * Actualiza la ficha de la tienda del vendedor autenticado.
   *
   * @param request datos editables.
   * @returns observable con la tienda actualizada.
   */
  public updateMine(request: StoreRequest): Observable<Store> {
    return this.http.put<Store>(`${this.storesUrl}/mine`, request);
  }
}
