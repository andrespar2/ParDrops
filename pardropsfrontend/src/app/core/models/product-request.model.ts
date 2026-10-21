/**
 * @file src/app/core/models/product-request.model.ts
 * @description Modelo de los datos que se envían a la API al crear o editar
 * un producto desde el panel de administración.
 *
 * Coincide con el DTO `ProductRequest` del backend. No incluye `id` ni las
 * fechas, que los gestiona el servidor.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { ProductImage, ProductVariant } from './product.model';

/**
 * Datos editables de un producto.
 *
 * @interface ProductRequest
 */
export interface ProductRequest {
  /** Nombre del producto. */
  name: string;
  /** Slug único para la URL. */
  slug: string;
  /** Descripción corta. */
  description: string;
  /** Descripción larga (opcional). */
  longDescription?: string | null;
  /** Precio actual. */
  price: number;
  /** Precio anterior, si está en oferta (opcional). */
  originalPrice?: number | null;
  /** Código de moneda (por defecto COP). */
  currency?: string;
  /** Imágenes del producto. */
  images?: ProductImage[];
  /** Categorías. */
  categories?: string[];
  /** Etiquetas de búsqueda. */
  tags?: string[];
  /** Unidades disponibles. */
  stock: number;
  /** Variantes. */
  variants?: ProductVariant[];
  /** Valoración media (opcional). */
  rating?: number | null;
  /** Número de reseñas (opcional). */
  reviewCount?: number | null;
  /** Marca de novedad. */
  isNew?: boolean;
  /** Marca de destacado. */
  isFeatured?: boolean;
  /** Marca de oferta. */
  onSale?: boolean;
}
