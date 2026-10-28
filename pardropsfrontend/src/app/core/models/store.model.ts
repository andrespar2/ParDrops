/**
 * @file src/app/core/models/store.model.ts
 * @description Modelos de las tiendas públicas de los vendedores.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

/** Estado de una tienda dentro del marketplace. */
export type StoreStatus = 'ACTIVE' | 'SUSPENDED';

/**
 * Ficha pública de la tienda de un vendedor.
 *
 * @interface Store
 */
export interface Store {
  /** Identificador de la tienda. */
  id: number;
  /** Nombre comercial. */
  name: string;
  /** Slug público usado en la URL. */
  slug: string;
  /** Descripción comercial. */
  description?: string | null;
  /** URL del logotipo. */
  logoUrl?: string | null;
  /** URL de la portada. */
  bannerUrl?: string | null;
  /** Correo público de la tienda. */
  publicEmail?: string | null;
  /** Teléfono público de la tienda. */
  publicPhone?: string | null;
  /** Ciudad de la tienda. */
  city?: string | null;
  /** WhatsApp de contacto. */
  whatsapp?: string | null;
  /** Estado de la tienda. */
  status: StoreStatus;
  /** Media de estrellas de sus productos. */
  rating: number;
  /** Número de reseñas de sus productos. */
  reviewCount: number;
  /** Número de productos publicados. */
  productCount: number;
  /** Fecha de alta (ISO 8601). */
  createdAt: string;
}

/**
 * Datos editables de la propia tienda.
 *
 * @interface StoreRequest
 */
export interface StoreRequest {
  /** Descripción comercial. */
  description?: string | null;
  /** URL del logotipo. */
  logoUrl?: string | null;
  /** URL de la portada. */
  bannerUrl?: string | null;
  /** Correo público de la tienda. */
  publicEmail?: string | null;
  /** Teléfono público de la tienda. */
  publicPhone?: string | null;
  /** WhatsApp de contacto. */
  whatsapp?: string | null;
  /** Ciudad de la tienda. */
  city?: string | null;
  /** Nombre comercial. */
  name?: string | null;
  /** Slug público (minúsculas, números y guiones). */
  slug?: string | null;
}
