/**
 * @file src/app/pages/product-list/product-list.component.ts
 * @description Página para listar y filtrar productos.
 *
 * Los filtros se envían al endpoint paginado del catálogo. La distribución se
 * mantiene estable mientras el usuario navega y cambia al recargar la página.
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../core/services/product.service';
import { CategoryService } from '../../core/services/category.service';
import { ProductFeedFilters } from '../../core/models/product.model';
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
  private readonly categoryService = inject(CategoryService);

  /** Término de búsqueda. */
  public searchQuery = signal<string>('');

  /** Categoría seleccionada. */
  public selectedCategory = signal<string>('all');

  /** Filtro activo (all, nuevos, ofertas). */
  public activeFilter = signal<string>('all');

  /** Categorías disponibles (desde la API). */
  public categories: string[] = [];

  /** Total de resultados del filtro actual. */
  public readonly totalElements = this.productService.totalElements;

  /** Productos ya filtrados por el servidor. */
  public readonly filteredProducts = computed(() => this.productService.products());

  /** Indica si quedan más páginas. */
  public readonly hasMore = this.productService.hasMore;

  /** Temporizador de búsqueda para no lanzar una petición por tecla. */
  private searchTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.categoryService.list().subscribe({
      next: categories => {
        this.categories = categories
          .filter(category => !category.collection)
          .map(category => category.name);
      },
      error: () => {
        this.categories = [];
      }
    });

    this.route.queryParams.subscribe(params => {
      this.selectedCategory.set(params['category'] ?? 'all');
      const filter = String(params['filter'] ?? 'all').toLowerCase();
      this.activeFilter.set(filter === 'nuevos' || filter === 'ofertas' ? filter : 'all');
      this.searchQuery.set(params['search'] ?? '');
      this.reload();
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
   * Actualiza la búsqueda con un pequeño retardo.
   * @param {string} query - Término de búsqueda.
   */
  public setSearch(query: string): void {
    this.searchQuery.set(query);
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    this.searchTimer = setTimeout(() => {
      this.updateQueryParams({ search: query.trim() || null });
    }, 250);
  }

  /** Carga la siguiente página conservando la distribución. */
  public loadMore(): void {
    this.productService.loadMore();
  }

  /** Limpia todos los filtros. */
  public clearFilters(): void {
    this.searchQuery.set('');
    this.selectedCategory.set('all');
    this.activeFilter.set('all');
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {}
    });
  }

  private reload(): void {
    const filters: ProductFeedFilters = {
      search: this.searchQuery().trim(),
      category: this.selectedCategory() === 'all' ? null : this.selectedCategory(),
      onlyNew: this.activeFilter() === 'nuevos',
      onlySale: this.activeFilter() === 'ofertas'
    };
    this.productService.loadProducts(filters);
  }

  /**
   * Actualiza los query params sin recargar la página.
   * @param params - Parámetros a actualizar.
   */
  private updateQueryParams(params: Record<string, string | null>): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge'
    });
  }
}
