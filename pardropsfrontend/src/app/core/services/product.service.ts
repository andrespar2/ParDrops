/**
 * @file src/app/core/services/product.service.ts
 * @description Servicio para la gestión y obtención de productos.
 * 
 * Este servicio proporciona métodos para obtener el catálogo completo de productos,
 * buscar productos por ID o slug, filtrar por categorías, y acceder a productos
 * destacados o en oferta. Incluye datos de ejemplo para el desarrollo.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Injectable, signal, computed } from '@angular/core';
import { Product } from '../models/product.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  /**
   * Signal privado que almacena la lista completa de productos.
   * @private
   */
  private readonly _products = signal<Product[]>([]);

  /**
   * Signal computado que expone la lista completa de productos.
   * @returns {Product[]} Lista de todos los productos.
   */
  public readonly products = computed(() => this._products());

  /**
   * Signal computado que obtiene los productos destacados.
   * @returns {Product[]} Lista de productos con isFeatured = true.
   */
  public readonly featuredProducts = computed(() => 
    this._products().filter(product => product.isFeatured)
  );

  /**
   * Signal computado que obtiene los productos nuevos.
   * @returns {Product[]} Lista de productos con isNew = true.
   */
  public readonly newProducts = computed(() => 
    this._products().filter(product => product.isNew)
  );

  /**
   * Signal computado que obtiene los productos en oferta.
   * @returns {Product[]} Lista de productos con onSale = true.
   */
  public readonly saleProducts = computed(() => 
    this._products().filter(product => product.onSale)
  );

  constructor() {
    this.loadProducts();
  }

  /**
   * Carga el catálogo de productos inicial.
   * En un entorno de producción, estos datos vendrían de una API REST.
   * @private
   */
  private loadProducts(): void {
    const products: Product[] = [
      {
        id: 'prod-001',
        name: 'Camiseta Minimalista Negra',
        slug: 'camiseta-minimalista-negra',
        description: 'Camiseta básica de algodón orgánico con diseño minimalista.',
        longDescription: 'Camiseta confeccionada en algodón orgánico de alta calidad. Diseño minimalista con corte moderno y unisex. Ideal para looks casuales o sofisticados.',
        price: 29.99,
        currency: 'EUR',
        images: [
          {
            url: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&h=800&fit=crop',
            alt: 'Camiseta minimalista negra',
            isPrimary: true
          },
          {
            url: 'https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=800&h=800&fit=crop',
            alt: 'Camiseta minimalista negra - vista lateral'
          }
        ],
        categories: ['Ropa', 'Camisetas'],
        tags: ['minimalista', 'negra', 'algodón'],
        stock: 50,
        rating: 4.8,
        reviewCount: 124,
        isNew: true,
        isFeatured: true,
        onSale: false,
        createdAt: new Date('2026-01-15')
      },
      {
        id: 'prod-002',
        name: 'Pantalón Cargo Slim',
        slug: 'pantalon-cargo-slim',
        description: 'Pantalón cargo de corte slim con múltiples bolsillos.',
        longDescription: 'Pantalón cargo moderno con corte slim fit. Confeccionado en mezcla de algodón y elastano para mayor comodidad. Múltiples bolsillos funcionales con diseño minimalista.',
        price: 59.99,
        originalPrice: 79.99,
        currency: 'EUR',
        images: [
          {
            url: 'https://images.unsplash.com/photo-1552374196-1ab2a1c593e8?w=800&h=800&fit=crop',
            alt: 'Pantalón cargo slim',
            isPrimary: true
          }
        ],
        categories: ['Ropa', 'Pantalones'],
        tags: ['cargo', 'slim', 'negro'],
        stock: 30,
        rating: 4.6,
        reviewCount: 89,
        isNew: false,
        isFeatured: true,
        onSale: true,
        createdAt: new Date('2025-12-10')
      },
      {
        id: 'prod-003',
        name: 'Zapatillas Urbanas Blancas',
        slug: 'zapatillas-urbanas-blancas',
        description: 'Zapatillas urbanas minimalistas en color blanco.',
        longDescription: 'Zapatillas de estilo urbano minimalista. Suela de goma resistente y exterior de lona premium. Diseño versátil que combina con cualquier look.',
        price: 79.99,
        currency: 'EUR',
        images: [
          {
            url: 'https://images.unsplash.com/photo-1549298916-b41d501d3772?w=800&h=800&fit=crop',
            alt: 'Zapatillas urbanas blancas',
            isPrimary: true
          },
          {
            url: 'https://images.unsplash.com/photo-1560769629-975ec94e6a86?w=800&h=800&fit=crop',
            alt: 'Zapatillas urbanas blancas - detalle'
          }
        ],
        categories: ['Calzado', 'Zapatillas'],
        tags: ['zapatillas', 'urbanas', 'blancas'],
        stock: 40,
        rating: 4.9,
        reviewCount: 256,
        isNew: true,
        isFeatured: true,
        onSale: false,
        createdAt: new Date('2026-02-01')
      },
      {
        id: 'prod-004',
        name: 'Mochila Minimalista',
        slug: 'mochila-minimalista',
        description: 'Mochila minimalista con compartimento para portátil.',
        longDescription: 'Mochila de diseño minimalista con capacidad para portátil de 15". Fabricada en nylon resistente al agua con acolchado ergonómico en espalda y correas.',
        price: 49.99,
        currency: 'EUR',
        images: [
          {
            url: 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&h=800&fit=crop',
            alt: 'Mochila minimalista',
            isPrimary: true
          }
        ],
        categories: ['Accesorios', 'Mochilas'],
        tags: ['mochila', 'minimalista', 'portátil'],
        stock: 25,
        rating: 4.7,
        reviewCount: 78,
        isNew: false,
        isFeatured: false,
        onSale: false,
        createdAt: new Date('2025-11-20')
      },
      {
        id: 'prod-005',
        name: 'Bufanda de Lana',
        slug: 'bufanda-de-lana',
        description: 'Bufanda de lana merino suave al tacto.',
        longDescription: 'Bufanda confeccionada en 100% lana merino. Suave, cálida y ligera. Diseño minimalista unisex disponible en varios colores.',
        price: 24.99,
        currency: 'EUR',
        images: [
          {
            url: 'https://images.unsplash.com/photo-1605236453806-6ff36851218e?w=800&h=800&fit=crop',
            alt: 'Bufanda de lana',
            isPrimary: true
          }
        ],
        categories: ['Accesorios', 'Bufandas'],
        tags: ['bufanda', 'lana', 'invierno'],
        stock: 60,
        rating: 4.5,
        reviewCount: 45,
        isNew: true,
        isFeatured: false,
        onSale: false,
        createdAt: new Date('2026-01-28')
      },
      {
        id: 'prod-006',
        name: 'Cazadora Bomber Negra',
        slug: 'cazadora-bomber-negra',
        description: 'Cazadora bomber minimalista con forro interior.',
        longDescription: 'Cazadora bomber de diseño minimalista. Exterior resistente al viento con forro acolchado. Bolsillos frontales y ribetes elásticos en puños y cintura.',
        price: 89.99,
        originalPrice: 109.99,
        currency: 'EUR',
        images: [
          {
            url: 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&h=800&fit=crop',
            alt: 'Cazadora bomber negra',
            isPrimary: true
          }
        ],
        categories: ['Ropa', 'Chaquetas'],
        tags: ['bomber', 'negra', 'minimalista'],
        stock: 15,
        rating: 4.8,
        reviewCount: 112,
        isNew: false,
        isFeatured: true,
        onSale: true,
        createdAt: new Date('2025-12-05')
      }
    ];

    this._products.set(products);
  }

  /**
   * Obtiene un producto por su ID.
   * @param {string} id - Identificador único del producto.
   * @returns {Product | undefined} El producto encontrado o undefined si no existe.
   */
  getProductById(id: string): Product | undefined {
    return this._products().find(product => product.id === id);
  }

  /**
   * Obtiene un producto por su slug.
   * @param {string} slug - Slug del producto.
   * @returns {Product | undefined} El producto encontrado o undefined si no existe.
   */
  getProductBySlug(slug: string): Product | undefined {
    return this._products().find(product => product.slug === slug);
  }

  /**
   * Obtiene todos los productos pertenecientes a una categoría.
   * @param {string} category - Nombre de la categoría.
   * @returns {Product[]} Lista de productos filtrados por categoría.
   */
  getProductsByCategory(category: string): Product[] {
    return this._products().filter(product => 
      product.categories.some(cat => cat.toLowerCase() === category.toLowerCase())
    );
  }

  /**
   * Busca productos por término de búsqueda.
   * @param {string} query - Término de búsqueda.
   * @returns {Product[]} Lista de productos que coinciden con la búsqueda.
   */
  searchProducts(query: string): Product[] {
    const lowerQuery = query.toLowerCase().trim();
    if (!lowerQuery) return this._products();
    
    return this._products().filter(product => 
      product.name.toLowerCase().includes(lowerQuery) ||
      product.description.toLowerCase().includes(lowerQuery) ||
      product.tags?.some(tag => tag.toLowerCase().includes(lowerQuery)) ||
      product.categories.some(cat => cat.toLowerCase().includes(lowerQuery))
    );
  }
}