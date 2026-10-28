/**
 * @file src/app/core/models/admin.model.ts
 * @description Modelos del panel de administración.
 *
 * Recoge los tipos que devuelve la API de administración (cuentas, vendedores,
 * productos, categorías, tablero, solicitudes y auditoría) y los tipos de las
 * peticiones que envía el panel. Se mantienen aquí centralizados para que las
 * páginas del panel compartan exactamente la misma forma de datos.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

/** Rol de una cuenta dentro del marketplace. */
export type AdminRole = 'CUSTOMER' | 'SELLER' | 'ADMIN';

/** Estado de una cuenta de usuario. */
export type AdminUserStatus = 'ACTIVE' | 'SUSPENDED';

/** Estado de la tienda de un vendedor. */
export type AdminStoreStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING';

/**
 * Página de resultados devuelta por los endpoints paginados.
 *
 * @interface PageResponse
 */
export interface PageResponse<T> {
  /** Elementos de la página actual. */
  content: T[];
  /** Número de página (empezando en 0). */
  page: number;
  /** Tamaño de página solicitado. */
  size: number;
  /** Total de elementos que cumplen el filtro. */
  totalElements: number;
  /** Total de páginas disponibles. */
  totalPages: number;
}

/**
 * Cuenta tal y como aparece en el listado del panel.
 *
 * @interface AdminUser
 */
export interface AdminUser {
  /** Identificador de la cuenta. */
  id: number;
  /** Correo de acceso. */
  email: string;
  /** Nombre completo. */
  fullName: string;
  /** Teléfono de contacto. */
  phone: string | null;
  /** Rol asignado. */
  role: AdminRole;
  /** Estado de la cuenta. */
  status: AdminUserStatus;
  /** Nombre de la tienda (nulo si no es vendedor). */
  storeName: string | null;
  /** Fecha de alta. */
  createdAt: string;
}

/**
 * Ficha completa de una cuenta, con su tienda y su actividad.
 *
 * @interface AdminUserDetail
 */
export interface AdminUserDetail extends Omit<AdminUser, 'storeName'> {
  /** Identificador de la tienda (nulo si no es vendedor). */
  storeId: number | null;
  /** Nombre de la tienda (nulo si no es vendedor). */
  storeName: string | null;
  /** Slug público de la tienda (nulo si no es vendedor). */
  storeSlug: string | null;
  /** Estado de la tienda (nulo si no es vendedor). */
  storeStatus: AdminStoreStatus | null;
  /** Productos publicados por el vendedor. */
  productCount: number;
  /** Ventas recibidas por el vendedor. */
  salesCount: number;
  /** Importe total vendido por el vendedor. */
  salesTotal: number;
}

/**
 * Vendedor con su tienda y sus cifras de actividad.
 *
 * @interface AdminSeller
 */
export interface AdminSeller {
  /** Identificador del vendedor. */
  sellerId: number;
  /** Nombre del vendedor. */
  fullName: string;
  /** Correo del vendedor. */
  email: string;
  /** Teléfono del vendedor. */
  phone: string | null;
  /** Nombre legal del titular (solo admin). */
  legalName: string | null;
  /** Tipo de documento (solo admin). */
  documentType: string | null;
  /** Número de documento (solo admin). */
  documentNumber: string | null;
  /** Dirección legal (solo admin). */
  legalAddress: string | null;
  /** NIT o identificación tributaria. */
  taxId: string | null;
  /** Ciudad pública de la tienda. */
  city: string | null;
  /** Correo público de la tienda. */
  publicEmail: string | null;
  /** Teléfono público de la tienda. */
  publicPhone: string | null;
  /** Estado de la cuenta del vendedor. */
  accountStatus: AdminUserStatus;
  /** Identificador de la tienda. */
  storeId: number;
  /** Nombre comercial de la tienda. */
  storeName: string;
  /** Slug público de la tienda. */
  storeSlug: string;
  /** Estado de la tienda. */
  storeStatus: AdminStoreStatus;
  /** Productos publicados. */
  productCount: number;
  /** Ventas recibidas. */
  salesCount: number;
  /** Importe total vendido. */
  salesTotal: number;
  /** Comisión acumulada de la plataforma. */
  commissionTotal: number;
  /** Neto retenido hasta la entrega. */
  pendingBalance: number;
  /** Neto disponible para el vendedor. */
  availableBalance: number;
  /** Fecha de alta de la cuenta. */
  createdAt: string;
}

/**
 * Producto con su propietario para la moderación del catálogo.
 *
 * @interface AdminProduct
 */
export interface AdminProduct {
  /** Identificador del producto. */
  id: string;
  /** Nombre visible. */
  name: string;
  /** Slug del producto. */
  slug: string;
  /** Descripción corta. */
  description: string;
  /** Precio actual. */
  price: number;
  /** Código de moneda. */
  currency: string;
  /** Unidades disponibles. */
  stock: number;
  /** Indica si está publicado en el catálogo. */
  active: boolean;
  /** Categorías del producto. */
  categories: string[];
  /** URL de la imagen principal (nulo si no tiene imágenes). */
  imageUrl: string | null;
  /** Identificador del vendedor (nulo si es de la plataforma). */
  sellerId: number | null;
  /** Nombre del vendedor (nulo si es de la plataforma). */
  sellerName: string | null;
  /** Correo del vendedor (nulo si es de la plataforma). */
  sellerEmail: string | null;
  /** Identificador de la tienda del vendedor. */
  storeId: number | null;
  /** Nombre de la tienda del vendedor. */
  storeName: string | null;
  /** Slug de la tienda del vendedor. */
  storeSlug: string | null;
  /** Estado de la tienda del vendedor. */
  storeStatus: AdminStoreStatus | null;
  /** Estado de la cuenta del vendedor. */
  sellerStatus: AdminUserStatus | null;
  /** Fecha de alta. */
  createdAt: string;
  /** Fecha de la última modificación. */
  updatedAt: string;
}

/**
 * Categoría del catálogo.
 *
 * @interface AdminCategory
 */
export interface AdminCategory {
  /** Identificador de la categoría. */
  id: number;
  /** Nombre visible. */
  name: string;
  /** Slug de la categoría. */
  slug: string;
  /** Descripción. */
  description: string | null;
  /** Indica si está disponible para asignarla. */
  active: boolean;
  /** Indica si se muestra en el menú del catálogo. */
  featured: boolean;
  /** Productos que usan la categoría. */
  productCount: number;
  /** Fecha de alta. */
  createdAt: string;
  /** Fecha de la última modificación. */
  updatedAt: string;
}

/**
 * Datos que se envían al crear o editar una categoría.
 *
 * @interface CategoryRequest
 */
export interface CategoryRequest {
  /** Nombre de la categoría. */
  name: string;
  /** Slug (opcional; la API lo genera del nombre). */
  slug: string | null;
  /** Descripción (opcional). */
  description: string | null;
  /** Indica si está disponible. */
  active: boolean;
  /** Indica si se muestra en el menú. */
  featured: boolean;
}

/**
 * Pedido reciente mostrado en el tablero.
 *
 * @interface AdminDashboardOrder
 */
export interface AdminDashboardOrder {
  /** Identificador del pedido. */
  id: number;
  /** Nombre del cliente. */
  customerName: string;
  /** Importe total. */
  total: number;
  /** Estado actual. */
  status: string;
  /** Fecha del pedido. */
  createdAt: string;
}

/**
 * Tienda con actividad comercial mostrada en el tablero.
 *
 * @interface AdminDashboardStore
 */
export interface AdminDashboardStore {
  /** Nombre de la tienda. */
  storeName: string;
  /** Slug público de la tienda. */
  storeSlug: string;
  /** Ventas recibidas. */
  salesCount: number;
  /** Importe total vendido. */
  salesTotal: number;
}

/**
 * Resumen general de la plataforma para el tablero del panel.
 *
 * @interface AdminDashboard
 */
export interface AdminDashboard {
  /** Cuentas registradas. */
  userCount: number;
  /** Cuentas con rol CLIENTE. */
  customerCount: number;
  /** Cuentas con rol SELLER. */
  sellerCount: number;
  /** Vendedores con tienda activa. */
  activeSellerCount: number;
  /** Cuentas suspendidas. */
  suspendedUserCount: number;
  /** Productos del catálogo. */
  productCount: number;
  /** Productos publicados. */
  publishedProductCount: number;
  /** Productos sin existencias. */
  outOfStockCount: number;
  /** Tiendas registradas. */
  storeCount: number;
  /** Tiendas activas. */
  activeStoreCount: number;
  /** Pedidos realizados. */
  orderCount: number;
  /** Partes de pedido sin finalizar. */
  pendingOrderCount: number;
  /** Importe total vendido. */
  revenue: number;
  /** Comisión acumulada de la plataforma. */
  commissionRevenue: number;
  /** Neto retenido a los vendedores. */
  retainedBalance: number;
  /** Neto disponible de los vendedores. */
  availableBalance: number;
  /** Solicitudes de vendedor por revisar. */
  pendingApplicationCount: number;
  /** Últimos pedidos registrados. */
  recentOrders: AdminDashboardOrder[];
  /** Tiendas con más ventas. */
  topStores: AdminDashboardStore[];
}

/** Estado de una solicitud para ser vendedor. */
export type SellerApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

/**
 * Solicitud para convertirse en vendedor.
 *
 * @interface SellerApplication
 */
export interface SellerApplication {
  /** Identificador de la solicitud. */
  id: number;
  /** Nombre comercial del negocio. */
  businessName: string;
  /** NIT o identificación tributaria. */
  taxId: string;
  /** Descripción del negocio. */
  description: string;
  /** Teléfono de contacto. */
  phone: string;
  /** Ciudad del negocio. */
  city: string;
  /** Estado de la solicitud. */
  status: SellerApplicationStatus;
  /** Comentario del administrador. */
  reviewNotes: string | null;
  /** Nombre del solicitante. */
  applicantName: string;
  /** Correo del solicitante. */
  applicantEmail: string;
  /** Administrador que revisó la solicitud. */
  reviewedBy: string | null;
  /** Fecha de la revisión. */
  reviewedAt: string | null;
  /** Fecha de envío. */
  createdAt: string;
}

/**
 * Decisión del administrador sobre una solicitud.
 *
 * @interface SellerApplicationDecision
 */
export interface SellerApplicationDecision {
  /** `true` para aprobar, `false` para rechazar. */
  approve: boolean;
  /** Motivo de la decisión. */
  reviewNotes: string | null;
}

/**
 * Registro de la bitácora de auditoría.
 *
 * @interface AuditLog
 */
export interface AuditLog {
  /** Identificador del registro. */
  id: number;
  /** Administrador que ejecutó la acción. */
  actorName: string;
  /** Tipo de acción ejecutada. */
  action: string;
  /** Tipo de entidad afectada. */
  entityType: string;
  /** Identificador de la entidad afectada. */
  entityId: string | null;
  /** Detalle legible de la acción. */
  details: string;
  /** Fecha de la acción. */
  createdAt: string;
}

/**
 * Códigos de las plantillas de correo.
 *
 * @type EmailTemplateCode
 */
export type EmailTemplateCode =
  | 'SELLER_NEW_ORDER'
  | 'BUYER_PAYMENT_RECEIPT'
  | 'BUYER_ORDER_STATUS';

/**
 * Plantilla de correo editable desde el panel.
 *
 * @interface EmailTemplate
 */
export interface EmailTemplate {
  /** Identificador. */
  id: number;
  /** Código del correo. */
  code: EmailTemplateCode;
  /** Nombre visible. */
  name: string;
  /** Explicación de cuándo se envía. */
  description: string | null;
  /** Asunto. */
  subject: string;
  /** Cuerpo HTML con variables. */
  bodyHtml: string;
  /** Indica si está activa. */
  active: boolean;
  /** Última modificación. */
  updatedAt: string | null;
}

/**
 * Datos editables de una plantilla.
 *
 * @interface EmailTemplateRequest
 */
export interface EmailTemplateRequest {
  /** Asunto del correo. */
  subject: string;
  /** Cuerpo HTML. */
  bodyHtml: string;
  /** Indica si está activa. */
  active: boolean;
}

/**
 * Correo renderizado para la vista previa.
 *
 * @interface RenderedEmail
 */
export interface RenderedEmail {
  /** Asunto final. */
  subject: string;
  /** Cuerpo HTML final. */
  bodyHtml: string;
}
