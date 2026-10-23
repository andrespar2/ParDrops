/**
 * @file src/app/core/services/seller.service.ts
 * @description Servicio del panel privado del vendedor.
 *
 * Agrupa el resumen de actividad, la gestión de sus productos, la preparación
 * de sus subpedidos y la solicitud para convertirse en vendedor. El backend
 * filtra siempre por el vendedor autenticado, de modo que ninguna llamada
 * puede operar sobre datos de otra tienda.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Product } from '../models/product.model';
import { ProductRequest } from '../models/product-request.model';
import { OrderStatus } from '../models/order.model';
import {
  SellerApplication,
  SellerApplicationRequest,
  SellerDashboard,
  SellerOrder
} from '../models/seller.model';

@Injectable({
  providedIn: 'root'
})
export class SellerService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** URL del resumen del panel. */
  private readonly dashboardUrl = `${API_BASE_URL}/seller/dashboard`;

  /** URL de los productos del vendedor. */
  private readonly productsUrl = `${API_BASE_URL}/seller/products`;

  /** URL de los subpedidos del vendedor. */
  private readonly ordersUrl = `${API_BASE_URL}/seller/orders`;

  /** URL de las solicitudes para vender. */
  private readonly applicationsUrl = `${API_BASE_URL}/seller-applications`;

  /**
   * Obtiene el resumen de actividad del vendedor autenticado.
   *
   * @returns observable con los indicadores del panel.
   */
  public getDashboard(): Observable<SellerDashboard> {
    return this.http.get<SellerDashboard>(this.dashboardUrl);
  }

  /**
   * Lista los productos del vendedor autenticado (publicados y ocultos).
   *
   * @returns observable con sus productos.
   */
  public listProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(this.productsUrl);
  }

  /**
   * Crea un producto en la tienda del vendedor.
   *
   * @param request datos del producto.
   * @returns observable con el producto creado.
   */
  public createProduct(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.productsUrl, request);
  }

  /**
   * Actualiza un producto propio.
   *
   * @param id      identificador del producto.
   * @param request datos actualizados.
   * @returns observable con el producto actualizado.
   */
  public updateProduct(id: string, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.productsUrl}/${id}`, request);
  }

  /**
   * Elimina un producto propio.
   *
   * @param id identificador del producto.
   * @returns observable que completa al borrar.
   */
  public deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.productsUrl}/${id}`);
  }

  /**
   * Publica u oculta un producto propio.
   *
   * @param id     identificador del producto.
   * @param active {@code true} para publicarlo, {@code false} para ocultarlo.
   * @returns observable con el producto actualizado.
   */
  public setProductVisibility(id: string, active: boolean): Observable<Product> {
    const params = new HttpParams().set('active', String(active));
    return this.http.patch<Product>(
      `${this.productsUrl}/${id}/visibility`, null, { params });
  }

  /**
   * Lista las partes de pedido que corresponden al vendedor.
   *
   * @returns observable con sus subpedidos.
   */
  public listOrders(): Observable<SellerOrder[]> {
    return this.http.get<SellerOrder[]>(this.ordersUrl);
  }

  /**
   * Avanza el estado de una parte de pedido propia.
   *
   * @param id     identificador del subpedido.
   * @param status nuevo estado.
   * @returns observable con el subpedido actualizado.
   */
  public updateOrderStatus(id: number, status: OrderStatus): Observable<SellerOrder> {
    return this.http.patch<SellerOrder>(
      `${this.ordersUrl}/${id}/status`, { status });
  }

  /**
   * Consulta la solicitud de vendedor del usuario autenticado.
   *
   * @returns observable con la solicitud, o error 404 si no existe.
   */
  public getMyApplication(): Observable<SellerApplication> {
    return this.http.get<SellerApplication>(`${this.applicationsUrl}/me`);
  }

  /**
   * Envía una solicitud para convertirse en vendedor.
   *
   * @param request datos del negocio.
   * @returns observable con la solicitud creada.
   */
  public submitApplication(
    request: SellerApplicationRequest
  ): Observable<SellerApplication> {
    return this.http.post<SellerApplication>(this.applicationsUrl, request);
  }
}
