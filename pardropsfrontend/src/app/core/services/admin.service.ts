/**
 * @file src/app/core/services/admin.service.ts
 * @description Servicio del panel de administración.
 *
 * Agrupa las llamadas a la API de administración: tablero, cuentas, vendedores,
 * productos del catálogo global, categorías, solicitudes de vendedor y
 * auditoría. Todas las operaciones requieren rol ADMIN; la autorización real la
 * aplica el backend.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  AdminCategory,
  AdminDashboard,
  AdminProduct,
  AdminSeller,
  AdminUser,
  AdminUserDetail,
  AdminRole,
  AdminUserStatus,
  AdminStoreStatus,
  AuditLog,
  CategoryRequest,
  EmailTemplate,
  EmailTemplateCode,
  EmailTemplateRequest,
  PageResponse,
  RenderedEmail,
  SellerApplication,
  SellerApplicationDecision,
  SellerApplicationStatus
} from '../models/admin.model';
import { ProductRequest } from '../models/product-request.model';

/**
 * Filtros del listado de cuentas.
 *
 * @interface AdminUserFilters
 */
export interface AdminUserFilters {
  /** Texto de búsqueda por correo, nombre o teléfono. */
  search?: string;
  /** Filtro por rol. */
  role?: AdminRole | null;
  /** Filtro por estado. */
  status?: AdminUserStatus | null;
  /** Número de página (empezando en 0). */
  page?: number;
  /** Tamaño de página. */
  size?: number;
}

/**
 * Filtros del listado global de productos.
 *
 * @interface AdminProductFilters
 */
export interface AdminProductFilters {
  /** Texto de búsqueda por nombre, slug o descripción. */
  search?: string;
  /** Filtro por vendedor propietario. */
  sellerId?: number | null;
  /** Filtro por categoría. */
  category?: string | null;
  /** Filtro por visibilidad. */
  active?: boolean | null;
  /** Número de página (empezando en 0). */
  page?: number;
  /** Tamaño de página. */
  size?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** URL base del panel de administración. */
  private readonly adminUrl = `${API_BASE_URL}/admin`;

  // ------------------------------------------------------------------
  // Tablero (resumen general).
  // ------------------------------------------------------------------

  /**
   * Carga el resumen general de la plataforma.
   *
   * @returns observable con los indicadores del tablero.
   */
  public loadDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(`${this.adminUrl}/dashboard`);
  }

  // ------------------------------------------------------------------
  // Cuentas de usuario.
  // ------------------------------------------------------------------

  /**
   * Lista las cuentas paginadas según los filtros indicados.
   *
   * @param filters búsqueda, rol, estado y paginación.
   * @returns observable con la página de cuentas.
   */
  public listUsers(filters: AdminUserFilters = {}): Observable<PageResponse<AdminUser>> {
    return this.http.get<PageResponse<AdminUser>>(
      `${this.adminUrl}/users`,
      { params: this.userParams(filters) }
    );
  }

  /**
   * Obtiene la ficha completa de una cuenta.
   *
   * @param id identificador de la cuenta.
   * @returns observable con la ficha de la cuenta.
   */
  public getUser(id: number): Observable<AdminUserDetail> {
    return this.http.get<AdminUserDetail>(`${this.adminUrl}/users/${id}`);
  }

  /**
   * Suspende o reactiva una cuenta.
   *
   * @param id     identificador de la cuenta.
   * @param status nuevo estado.
   * @param reason motivo del cambio.
   * @returns observable con la cuenta actualizada.
   */
  public changeUserStatus(
    id: number,
    status: AdminUserStatus,
    reason: string | null
  ): Observable<AdminUser> {
    return this.http.patch<AdminUser>(
      `${this.adminUrl}/users/${id}/status`,
      { status, reason }
    );
  }

  /**
   * Lista los vendedores con su tienda y su actividad.
   *
   * @param status filtro por estado de la tienda.
   * @param search texto de búsqueda.
   * @returns observable con el listado de vendedores.
   */
  public listSellers(
    status: AdminStoreStatus | null = null,
    search = ''
  ): Observable<AdminSeller[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (search.trim()) params = params.set('search', search.trim());
    return this.http.get<AdminSeller[]>(`${this.adminUrl}/sellers`, { params });
  }

  // ------------------------------------------------------------------
  // Catálogo global de productos.
  // ------------------------------------------------------------------

  /**
   * Lista el catálogo global paginado según los filtros indicados.
   *
   * @param filters búsqueda, vendedor, categoría, visibilidad y paginación.
   * @returns observable con la página de productos.
   */
  public listProducts(
    filters: AdminProductFilters = {}
  ): Observable<PageResponse<AdminProduct>> {
    return this.http.get<PageResponse<AdminProduct>>(
      `${this.adminUrl}/products`,
      { params: this.productParams(filters) }
    );
  }

  /**
   * Obtiene la ficha de un producto con su propietario.
   *
   * @param id identificador del producto.
   * @returns observable con la ficha del producto.
   */
  public getProduct(id: string): Observable<AdminProduct> {
    return this.http.get<AdminProduct>(`${this.adminUrl}/products/${id}`);
  }

  /**
   * Publica u oculta un producto del catálogo.
   *
   * @param id     identificador del producto.
   * @param active `true` para publicarlo, `false` para ocultarlo.
   * @returns observable con el producto actualizado.
   */
  public changeProductVisibility(id: string, active: boolean): Observable<unknown> {
    const params = new HttpParams().set('active', String(active));
    return this.http.patch(`${this.adminUrl}/products/${id}/visibility`, {}, { params });
  }

  /**
   * Elimina un producto del catálogo.
   *
   * @param id identificador del producto.
   * @returns observable que completa al borrar.
   */
  public deleteProduct(id: string): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/products/${id}`);
  }

  /**
   * Crea un producto de la plataforma (sin vendedor asignado).
   *
   * @param request datos del producto.
   * @returns observable con el producto creado.
   */
  public createProduct(request: ProductRequest): Observable<unknown> {
    return this.http.post(`${this.adminUrl}/products`, request);
  }

  /**
   * Actualiza cualquier producto del catálogo.
   *
   * @param id      identificador del producto.
   * @param request nuevos datos.
   * @returns observable con el producto actualizado.
   */
  public updateProduct(id: string, request: ProductRequest): Observable<unknown> {
    return this.http.put(`${this.adminUrl}/products/${id}`, request);
  }

  // ------------------------------------------------------------------
  // Categorías.
  // ------------------------------------------------------------------

  /**
   * Lista las categorías del catálogo.
   *
   * @param activeOnly si es `true` solo devuelve las activas.
   * @returns observable con el listado de categorías.
   */
  public listCategories(activeOnly = false): Observable<AdminCategory[]> {
    const params = new HttpParams().set('activeOnly', String(activeOnly));
    return this.http.get<AdminCategory[]>(`${this.adminUrl}/categories`, { params });
  }

  /**
   * Crea una categoría nueva.
   *
   * @param request datos de la categoría.
   * @returns observable con la categoría creada.
   */
  public createCategory(request: CategoryRequest): Observable<AdminCategory> {
    return this.http.post<AdminCategory>(`${this.adminUrl}/categories`, request);
  }

  /**
   * Actualiza una categoría existente.
   *
   * @param id      identificador de la categoría.
   * @param request nuevos datos.
   * @returns observable con la categoría actualizada.
   */
  public updateCategory(id: number, request: CategoryRequest): Observable<AdminCategory> {
    return this.http.put<AdminCategory>(`${this.adminUrl}/categories/${id}`, request);
  }

  /**
   * Elimina una categoría que no esté en uso.
   *
   * @param id identificador de la categoría.
   * @returns observable que completa al borrar.
   */
  public deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/categories/${id}`);
  }

  // ------------------------------------------------------------------
  // Solicitudes de vendedor.
  // ------------------------------------------------------------------

  /**
   * Lista las solicitudes para ser vendedor.
   *
   * @param status filtro opcional por estado.
   * @returns observable con el listado de solicitudes.
   */
  public listApplications(
    status: SellerApplicationStatus | null = null
  ): Observable<SellerApplication[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<SellerApplication[]>(
      `${API_BASE_URL}/seller-applications`,
      { params }
    );
  }

  /**
   * Aprueba o rechaza una solicitud de vendedor.
   *
   * @param id       identificador de la solicitud.
   * @param decision decisión del administrador.
   * @returns observable con la solicitud revisada.
   */
  public decideApplication(
    id: number,
    decision: SellerApplicationDecision
  ): Observable<SellerApplication> {
    return this.http.patch<SellerApplication>(
      `${API_BASE_URL}/seller-applications/${id}/decision`,
      decision
    );
  }

  // ------------------------------------------------------------------
  // Plantillas de correo.
  // ------------------------------------------------------------------

  /**
   * Lista las plantillas de correo.
   *
   * @returns observable con las plantillas.
   */
  public listEmailTemplates(): Observable<EmailTemplate[]> {
    return this.http.get<EmailTemplate[]>(`${this.adminUrl}/email-templates`);
  }

  /**
   * Obtiene una plantilla por su código.
   *
   * @param code código del correo.
   * @returns observable con la plantilla.
   */
  public getEmailTemplate(code: EmailTemplateCode): Observable<EmailTemplate> {
    return this.http.get<EmailTemplate>(`${this.adminUrl}/email-templates/${code}`);
  }

  /**
   * Actualiza una plantilla.
   *
   * @param code    código del correo.
   * @param request nuevos valores.
   * @returns observable con la plantilla actualizada.
   */
  public updateEmailTemplate(
    code: EmailTemplateCode,
    request: EmailTemplateRequest
  ): Observable<EmailTemplate> {
    return this.http.put<EmailTemplate>(`${this.adminUrl}/email-templates/${code}`, request);
  }

  /**
   * Restaura los valores predeterminados de una plantilla.
   *
   * @param code código del correo.
   * @returns observable con la plantilla restaurada.
   */
  public resetEmailTemplate(code: EmailTemplateCode): Observable<EmailTemplate> {
    return this.http.post<EmailTemplate>(
      `${this.adminUrl}/email-templates/${code}/reset`,
      {}
    );
  }

  /**
   * Genera una vista previa con datos de ejemplo.
   *
   * @param code código del correo.
   * @returns observable con el asunto y el HTML.
   */
  public previewEmailTemplate(code: EmailTemplateCode): Observable<RenderedEmail> {
    return this.http.post<RenderedEmail>(
      `${this.adminUrl}/email-templates/${code}/preview`,
      {}
    );
  }

  // ------------------------------------------------------------------
  // Auditoría.
  // ------------------------------------------------------------------

  /**
   * Carga los registros más recientes de la bitácora de auditoría.
   *
   * @returns observable con el listado de registros.
   */
  public listAuditLogs(): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.adminUrl}/audit-logs`);
  }

  // ------------------------------------------------------------------
  // Utilidades internas.
  // ------------------------------------------------------------------

  /**
   * Construye los parámetros de consulta del listado de cuentas.
   *
   * @param filters filtros del listado.
   * @returns los parámetros HTTP.
   * @private
   */
  private userParams(filters: AdminUserFilters): HttpParams {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20));

    if (filters.search?.trim()) params = params.set('search', filters.search.trim());
    if (filters.role) params = params.set('role', filters.role);
    if (filters.status) params = params.set('status', filters.status);

    return params;
  }

  /**
   * Construye los parámetros de consulta del listado de productos.
   *
   * @param filters filtros del listado.
   * @returns los parámetros HTTP.
   * @private
   */
  private productParams(filters: AdminProductFilters): HttpParams {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20));

    if (filters.search?.trim()) params = params.set('search', filters.search.trim());
    if (filters.sellerId != null) params = params.set('sellerId', String(filters.sellerId));
    if (filters.category) params = params.set('category', filters.category);
    if (filters.active != null) params = params.set('active', String(filters.active));

    return params;
  }
}
