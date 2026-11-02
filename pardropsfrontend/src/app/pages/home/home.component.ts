/**
 * @file src/app/pages/home/home.component.ts
 * @description Escaparate de productos con filtros y carga progresiva.
 *
 * La portada muestra el catálogo como contenido principal: buscador en la
 * cabecera, filtros en una barra lateral plegable y una cuadrícula amplia.
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { AuthService } from '../../core/services/auth.service';
import { Product } from '../../core/models/product.model';
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

  /** Número de productos visibles (carga progresiva). */
  public readonly visibleCount = signal<number>(12);

  /** Estado de la barra lateral en escritorio. */
  public readonly sidebarOpen = signal<boolean>(true);

  /** Categorías reales del catálogo (desde la API). */
  public readonly categories = signal<Category[]>([]);

  /** Categorías destacadas para la portada. */
  public readonly featuredCategories = computed(() =>
    this.categories().filter(category => category.featured && !category.collection).slice(0, 8)
  );

  /** Catálogo filtrado y ordenado. */
  public readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const category = this.category();
    const range = this.priceRange();
    const quick = this.quickFilter();

    let result: Product[] = this.products();

    if (term) {
      result = result.filter(product =>
        product.name.toLowerCase().includes(term) ||
        (product.description ?? '').toLowerCase().includes(term) ||
        (product.tags ?? []).some(tag => tag.toLowerCase().includes(term))
      );
    }

    if (category !== 'all') {
      result = result.filter(product =>
        (product.categories ?? []).some(name => name.toLowerCase() === category.toLowerCase())
      );
    }

    result = result.filter(product => this.matchesRange(product.price, range));

    if (quick === 'nuevos') {
      result = result.filter(product => product.isNew);
    } else if (quick === 'ofertas') {
      result = result.filter(product => product.onSale);
    }

    const sort = this.sort();
    const sorted = [...result];
    if (sort === 'price-asc') {
      sorted.sort((a, b) => a.price - b.price);
    } else if (sort === 'price-desc') {
      sorted.sort((a, b) => b.price - a.price);
    } else if (sort === 'recent') {
      sorted.sort((a, b) => String(b.createdAt ?? '').localeCompare(String(a.createdAt ?? '')));
    }
    return sorted;
  });

  /** Productos que se muestran en la cuadrícula. */
  public readonly visible = computed(() => this.filtered().slice(0, this.visibleCount()));

  /** Indica si quedan productos por mostrar. */
  public readonly hasMore = computed(() => this.visibleCount() < this.filtered().length);

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
      this.visibleCount.set(12);
    });
  }

  /**
   * Aplica la búsqueda escrita y reinicia la paginación.
   */
  public applySearch(): void {
    this.visibleCount.set(12);
  }

  /**
   * Selecciona una categoría.
   *
   * @param name categoría o `all` para todas.
   */
  public selectCategory(name: string): void {
    this.category.set(name);
    this.visibleCount.set(12);
  }

  /**
   * Cambia el rango de precio.
   *
   * @param range rango seleccionado.
   */
  public selectRange(range: PriceRange): void {
    this.priceRange.set(range);
    this.visibleCount.set(12);
  }

  /**
   * Cambia el orden del catálogo.
   *
   * @param value orden seleccionado.
   */
  public selectSort(value: string): void {
    this.sort.set(value as SortOption);
    this.visibleCount.set(12);
  }

  /** Muestra más productos. */
  public loadMore(): void {
    this.visibleCount.update(count => count + 12);
  }

  /** Limpia todos los filtros. */
  public clearFilters(): void {
    this.searchTerm.set('');
    this.category.set('all');
    this.priceRange.set('all');
    this.quickFilter.set('all');
    this.sort.set('relevance');
    this.visibleCount.set(12);
  }

  /** Recarga el catálogo desde la API. */
  public retry(): void {
    this.productService.loadProducts();
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

  /**
   * Comprueba si un precio cae en el rango seleccionado.
   *
   * @param price precio del producto.
   * @param range rango seleccionado.
   */
  private matchesRange(price: number, range: PriceRange): boolean {
    switch (range) {
      case 'lt100':
        return price < 100_000;
      case '100-300':
        return price >= 100_000 && price < 300_000;
      case '300-1000':
        return price >= 300_000 && price < 1_000_000;
      case 'gt1000':
        return price >= 1_000_000;
      default:
        return true;
    }
  }
}
