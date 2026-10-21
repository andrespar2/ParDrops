/**
 * @file src/app/core/models/product.model.ts
 * @description Define las interfaces y tipos relacionados con los productos del e-commerce.
 * 
 * Este archivo contiene las estructuras de datos que representan un producto,
 * sus variantes, y la información necesaria para su visualización y gestión
 * en el carrito de compras.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

/**
 * Interfaz que representa una imagen asociada a un producto.
 * 
 * @interface ProductImage
 * @property {string} url - URL de la imagen del producto.
 * @property {string} alt - Texto alternativo para accesibilidad.
 * @property {boolean} [isPrimary] - Indica si es la imagen principal del producto.
 */
export interface ProductImage {
  /** URL de la imagen del producto */
  url: string;
  /** Texto alternativo descriptivo para accesibilidad */
  alt: string;
  /** Indica si esta imagen es la principal (opcional) */
  isPrimary?: boolean;
}

/**
 * Interfaz que representa una variante de producto (talla, color, etc.).
 * 
 * @interface ProductVariant
 * @property {string} id - Identificador único de la variante.
 * @property {string} name - Nombre descriptivo de la variante (ej.: "Talla M", "Color Negro").
 * @property {number} [price] - Precio específico de la variante (si difiere del precio base).
 * @property {number} [stock] - Stock disponible para esta variante.
 * @property {string} [sku] - Código SKU único para la variante.
 */
export interface ProductVariant {
  /** Identificador único de la variante */
  id: string;
  /** Nombre de la variante */
  name: string;
  /** Precio específico de la variante (opcional) */
  price?: number;
  /** Stock disponible para esta variante (opcional) */
  stock?: number;
  /** Código SKU de la variante (opcional) */
  sku?: string;
}

/**
 * Interfaz principal que representa un producto en el catálogo.
 * 
 * Contiene toda la información detallada necesaria para mostrar el producto
 * en la lista, en su vista detallada y para su gestión en el carrito.
 * 
 * @interface Product
 * @property {string} id - Identificador único del producto.
 * @property {string} name - Nombre del producto.
 * @property {string} slug - Slug URL-friendly para navegación.
 * @property {string} description - Descripción corta del producto.
 * @property {string} [longDescription] - Descripción detallada del producto.
 * @property {number} price - Precio actual del producto.
 * @property {number} [originalPrice] - Precio original antes de descuento.
 * @property {string} currency - Moneda del precio (por defecto 'COP').
 * @property {ProductImage[]} images - Array con las imágenes del producto.
 * @property {string[]} categories - Categorías a las que pertenece el producto.
 * @property {string[]} [tags] - Etiquetas para filtrado y búsqueda.
 * @property {number} stock - Stock total disponible.
 * @property {ProductVariant[]} [variants] - Variantes disponibles del producto.
 * @property {number} [rating] - Puntuación promedio (0-5).
 * @property {number} [reviewCount] - Número de reseñas.
 * @property {boolean} isNew - Indica si el producto es nuevo.
 * @property {boolean} isFeatured - Indica si es destacado.
 * @property {boolean} onSale - Indica si tiene descuento.
 * @property {Date} [createdAt] - Fecha de creación.
 * @property {Date} [updatedAt] - Fecha de última actualización.
 */
export interface Product {
  /** Identificador único del producto */
  id: string;
  /** Nombre del producto */
  name: string;
  /** Slug para URLs amigables */
  slug: string;
  /** Descripción corta */
  description: string;
  /** Descripción completa y detallada (opcional) */
  longDescription?: string;
  /** Precio actual del producto */
  price: number;
  /** Precio original antes de descuentos (opcional) */
  originalPrice?: number;
  /** Código de moneda (default: 'COP') */
  currency: string;
  /** Lista de imágenes del producto */
  images: ProductImage[];
  /** Categorías del producto */
  categories: string[];
  /** Etiquetas para búsqueda/filtros (opcional) */
  tags?: string[];
  /** Stock disponible */
  stock: number;
  /** Variantes del producto (opcional) */
  variants?: ProductVariant[];
  /** Valoración promedio 0-5 (opcional) */
  rating?: number;
  /** Número de reseñas (opcional) */
  reviewCount?: number;
  /** Indica si es un producto nuevo */
  isNew: boolean;
  /** Indica si es un producto destacado */
  isFeatured: boolean;
  /** Indica si está en oferta */
  onSale: boolean;
  /** Fecha de creación (opcional) */
  createdAt?: Date;
  /** Fecha de actualización (opcional) */
  updatedAt?: Date;
}