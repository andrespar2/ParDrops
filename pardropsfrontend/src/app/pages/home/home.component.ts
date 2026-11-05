/**
 * @file src/app/pages/home/home.component.ts
 * @description Escaparate de productos con filtros y carga progresiva.
 *
 * Los filtros se resuelven en el servidor para no descargar todo el catálogo.
 * La primera página usa una semilla de distribución nueva en cada recarga y
 * las páginas siguientes conservan esa misma semilla.
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { AuthService } from '../../core/services/auth.service';
import { ProductFeedFilters } from '../../core/models/product.model';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { CategoryService } from '../../core/services/category.service';
import { Category } from '../../core/models/category.model';
import { formatCurrency } from '../../core/config/commerce.config';

/** Opciones de orden del catálogo. */
type SortOption = 'relevance' | 'price-asc' | 'price-desc' | 'recent';

/** Rangos de precio disponibles. */
type PriceRange = 'all' | 'lt100' | '100-300' | '300-1000' | 'gt1000';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ProductCardComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {
  /** Catálogo desde la API. */
  private readonly productService = inject(ProductService);

  /** Sesión del usuario, para accesos de vendedor. */
  private readonly authService = inject(AuthService);

  /** Ruta actual, para leer búsqueda y filtros de la URL. */
  private readonly route = inject(ActivatedRoute);

  /** Categorías del backend. */
  private readonly categoryService = inject(CategoryService);

  /** Productos cargados. */
  public readonly products = this.productService.products;

  /** Estado de carga del catálogo. */
  public readonly loading = this.productService.loading;

  /** Error de carga, si existe. */
  public readonly error = this.productService.error;

  /** Total de resultados del filtro actual. */
  public readonly totalElements = this.productService.totalElements;

  /** Indica si el usuario puede publicar productos. */
  public readonly isSeller = this.authService.isSeller;

  /** Texto de búsqueda. */
  public readonly searchTerm = signal<string>('');

  /** Categoría seleccionada. */
  public readonly category = signal<string>('all');

  /** Rango de precio seleccionado. */
  public readonly priceRange = signal<PriceRange>('all');

  /** Orden seleccionado. */
  public readonly sort = signal<SortOption>('relevance');

  /** Filtro rápido de la URL (nuevos, ofertas). */
  public readonly quickFilter = signal<'all' | 'nuevos' | 'ofertas'>('all');

  /** Estado de la barra lateral en escritorio. */
  public readonly sidebarOpen = signal<boolean>(true);

  /** Categorías reales del catálogo (desde la API). */
  public readonly categories = signal<Category[]>([]);

  /** Texto del buscador de categorías. */
  public readonly categorySearch = signal<string>('');

  /** Indica si se muestran todas las categorías. */
  public readonly categoriesExpanded = signal<boolean>(false);

  /** Categorías visibles según búsqueda y despliegue. */
  public readonly visibleCategories = computed(() => {
    const term = this.categorySearch().trim().toLowerCase();
    const all = this.categories();
    if (term) {
      return all.filter(category => category.name.toLowerCase().includes(term));
    }
    return this.categoriesExpanded() ? all : all.slice(0, 8);
  });

  /** Indica si hay más categorías de las visibles. */
  public readonly hasMoreCategories = computed(
    () => !this.categorySearch().trim() && this.categories().length > 8
  );

  /** Categorías destacadas para la portada. */
  public readonly featuredCategories = computed(() =>
    this.categories().filter(category => category.featured && !category.collection).slice(0, 8)
  );

  /** Productos ya filtrados y ordenados por el servidor. */
  public readonly filtered = computed(() => this.products());

  /** Productos que se muestran en la cuadrícula. */
  public readonly visible = computed(() => this.products());

  /** Indica si quedan páginas por mostrar. */
  public readonly hasMore = this.productService.hasMore;

  constructor() {
    this.categoryService.list().subscribe({
      next: categories => this.categories.set(categories.filter(category => !category.collection)),
      error: () => this.categories.set([])
    });

    this.route.queryParamMap.subscribe(params => {
      this.searchTerm.set(params.get('search') ?? '');
      this.category.set(params.get('category') ?? 'all');
      const filter = (params.get('filter') ?? '').toLowerCase();
      this.quickFilter.set(filter === 'nuevos' || filter === 'ofertas' ? filter : 'all');
      this.reload();
    });
  }

  /** Aplica la búsqueda escrita. */
  public applySearch(): void {
    this.reload();
  }

  /**
   * Selecciona una categoría.
   *
   * @param name categoría o `all` para todas.
   */
  public selectCategory(name: string): void {
    this.category.set(name);
    this.reload();
  }

  /**
   * Cambia el rango de precio.
   *
   * @param range rango seleccionado.
   */
  public selectRange(range: PriceRange): void {
    this.priceRange.set(range);
    this.reload();
  }

  /**
   * Cambia el orden del catálogo.
   *
   * @param value orden seleccionado.
   */
  public selectSort(value: string): void {
    this.sort.set(value as SortOption);
    this.reload();
  }

  /** Carga la siguiente página conservando la semilla. */
  public loadMore(): void {
    this.productService.loadMore();
  }

  /** Limpia todos los filtros. */
  public clearFilters(): void {
    this.searchTerm.set('');
    this.category.set('all');
    this.priceRange.set('all');
    this.quickFilter.set('all');
    this.sort.set('relevance');
    this.reload();
  }

  /** Recarga el catálogo con una distribución nueva. */
  public retry(): void {
    this.productService.reloadDistribution(this.buildFilters());
  }

  /** Alterna la barra lateral. */
  public toggleSidebar(): void {
    this.sidebarOpen.update(open => !open);
  }

  /**
   * Formatea un importe para las etiquetas de precio.
   *
   * @param value importe.
   */
  public formatPrice(value: number): string {
    return formatCurrency(value);
  }

  private reload(): void {
    this.productService.loadProducts(this.buildFilters());
  }

  private buildFilters(): ProductFeedFilters {
    const range = this.priceRange();
    const filters: ProductFeedFilters = {
      search: this.searchTerm().trim(),
      category: this.category() === 'all' ? null : this.category(),
      sort: this.sort()
    };

    switch (range) {
      case 'lt100':
        filters.maxPrice = 99_999;
        break;
      case '100-300':
        filters.minPrice = 100_000;
        filters.maxPrice = 299_999;
        break;
      case '300-1000':
        filters.minPrice = 300_000;
        filters.maxPrice = 999_999;
        break;
      case 'gt1000':
        filters.minPrice = 1_000_001;
        break;
      default:
        break;
    }

    if (this.quickFilter() === 'nuevos') filters.onlyNew = true;
    if (this.quickFilter() === 'ofertas') filters.onlySale = true;
    return filters;
  }
}
