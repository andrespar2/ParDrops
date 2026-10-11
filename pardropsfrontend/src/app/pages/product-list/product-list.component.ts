/**
 * @file src/app/pages/product-list/product-list.component.ts
 * @description Página para listar y filtrar productos.
 * 
 * Muestra catálogo completo con filtros por categoría, estado (nuevo/oferta)
 * y búsqueda. Incluye vista de cuadrícula.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../core/services/product.service';
import { Product } from '../../core/models/product.model';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductCardComponent],
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent {
  private readonly productService = inject(ProductService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  /** Término de búsqueda */
  public searchQuery = signal<string>('');

  /** Categoría seleccionada */
  public selectedCategory = signal<string>('all');

  /** Filtro activo (all, nuevos, ofertas) */
  public activeFilter = signal<string>('all');

  /** Categorías disponibles */
  public categories = ['Ropa', 'Calzado', 'Accesorios', 'Camisetas', 'Pantalones', 'Mochilas', 'Bufandas', 'Chaquetas', 'Zapatillas'];

  /** Productos filtrados computados */
  public readonly filteredProducts = computed(() => {
    let products: Product[] = this.productService.products();

    // Filtrar por búsqueda
    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      products = products.filter(product => 
        product.name.toLowerCase().includes(query) ||
        product.description.toLowerCase().includes(query) ||
        product.tags?.some(tag => tag.toLowerCase().includes(query))
      );
    }

    // Filtrar por categoría
    const category = this.selectedCategory();
    if (category !== 'all') {
      products = products.filter(product => 
        product.categories.some(cat => 
          cat.toLowerCase() === category.toLowerCase()
        )
      );
    }

    // Filtrar por estado
    const filter = this.activeFilter();
    if (filter === 'nuevos') {
      products = products.filter(product => product.isNew);
    } else if (filter === 'ofertas') {
      products = products.filter(product => product.onSale);
    }

    return products;
  });

  constructor() {
    // Suscribirse a los query params
    this.route.queryParams.subscribe(params => {
      if (params['category']) {
        this.selectedCategory.set(params['category']);
      }
      if (params['filter']) {
        const filter = params['filter'].toLowerCase();
        if (filter === 'nuevos' || filter === 'ofertas') {
          this.activeFilter.set(filter);
        }
      }
      if (params['search']) {
        this.searchQuery.set(params['search']);
      }
    });
  }

  /**
   * Actualiza el filtro de categoría.
   * @param {string} category - Categoría seleccionada.
   */
  public setCategory(category: string): void {
    this.selectedCategory.set(category);
    this.updateQueryParams({ category: category === 'all' ? null : category });
  }

  /**
   * Actualiza el filtro activo.
   * @param {string} filter - Filtro seleccionado.
   */
  public setFilter(filter: string): void {
    this.activeFilter.set(filter);
    this.updateQueryParams({ filter: filter === 'all' ? null : filter });
  }

  /**
   * Actualiza la búsqueda.
   * @param {string} query - Término de búsqueda.
   */
  public setSearch(query: string): void {
    this.searchQuery.set(query);
    this.updateQueryParams({ search: query.trim() || null });
  }

  /**
   * Limpia todos los filtros.
   */
  public clearFilters(): void {
    this.searchQuery.set('');
    this.selectedCategory.set('all');
    this.activeFilter.set('all');
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {}
    });
  }

  /**
   * Actualiza los query params sin recargar la página.
   * @param params - Parámetros a actualizar.
   */
  private updateQueryParams(params: any): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge'
    });
  }
}