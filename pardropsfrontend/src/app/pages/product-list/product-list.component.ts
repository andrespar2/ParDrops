/**
 * @file src/app/pages/product-list/product-list.component.ts
 * @description PÃ¡gina para listar y filtrar productos.
 * 
 * Muestra catÃ¡logo completo con filtros por categorÃ­a, estado (nuevo/oferta)
 * y bÃºsqueda. Incluye vista de cuadrÃ­cula.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../core/services/product.service';
import { CategoryService } from '../../core/services/category.service';
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

  /** Categorias del backend. */
  private readonly categoryService = inject(CategoryService);

  /** TÃ©rmino de bÃºsqueda */
  public searchQuery = signal<string>('');

  /** CategorÃ­a seleccionada */
  public selectedCategory = signal<string>('all');

  /** Filtro activo (all, nuevos, ofertas) */
  public activeFilter = signal<string>('all');

  /** Categorias disponibles (desde la API). */
  public categories: string[] = [];

  /** Productos filtrados computados */
  public readonly filteredProducts = computed(() => {
    let products: Product[] = this.productService.products();

    // Filtrar por bÃºsqueda
    const query = this.searchQuery().toLowerCase().trim();
    if (query) {
      products = products.filter(product => 
        product.name.toLowerCase().includes(query) ||
        product.description.toLowerCase().includes(query) ||
        product.tags?.some(tag => tag.toLowerCase().includes(query))
      );
    }

    // Filtrar por categorÃ­a
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
    this.categoryService.list().subscribe({
      next: categories => {
        this.categories = categories.filter(category => !category.collection).map(category => category.name);
      },
      error: () => {
        this.categories = [];
      }
    });

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
   * Actualiza el filtro de categorÃ­a.
   * @param {string} category - CategorÃ­a seleccionada.
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
   * Actualiza la bÃºsqueda.
   * @param {string} query - TÃ©rmino de bÃºsqueda.
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
   * Actualiza los query params sin recargar la pÃ¡gina.
   * @param params - ParÃ¡metros a actualizar.
   */
  private updateQueryParams(params: any): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge'
    });
  }
}

