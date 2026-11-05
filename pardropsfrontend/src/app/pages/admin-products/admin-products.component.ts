/**
 * @file src/app/pages/admin-products/admin-products.component.ts
 * @description Panel de administración del catálogo de productos.
 *
 * Permite al usuario con rol ADMIN crear, editar y eliminar productos. Los
 * cambios se envían a la API y la lista local se actualiza automáticamente.
 * Solo es accesible con rol de administrador (protegida por `adminGuard`).
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminService } from '../../core/services/admin.service';
import { ProductRequest } from '../../core/models/product-request.model';
import { Product, ProductImage, ProductVariant } from '../../core/models/product.model';
import {
  AdminCategory,
  AdminProduct,
  AdminStoreProductSummary,
  AdminStoreStatus,
  AdminUserStatus,
  PageResponse
} from '../../core/models/admin.model';
import { CURRENCY_CODE, formatCurrency } from '../../core/config/commerce.config';
import { ImageUploaderComponent } from '../../shared/components/image-uploader/image-uploader.component';
import { VariantEditorComponent } from '../../shared/components/variant-editor/variant-editor.component';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ImageUploaderComponent, VariantEditorComponent],
  templateUrl: './admin-products.component.html',
  styleUrls: ['./admin-products.component.scss']
})
export class AdminProductsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);

  /** Página del catálogo global devuelta por el panel de administración. */
  public readonly adminPage = signal<PageResponse<AdminProduct> | null>(null);

  /** Indica si el catálogo global se está cargando. */
  public readonly adminLoading = signal<boolean>(true);

  /** Tiendas con contadores de productos. */
  public readonly storePage = signal<PageResponse<AdminStoreProductSummary> | null>(null);

  /** Indica si se están cargando las tiendas. */
  public readonly storeLoading = signal<boolean>(true);

  /** Tienda seleccionada (nulo = catálogo propio). */
  public readonly selectedStore = signal<AdminStoreProductSummary | null>(null);

  /** Productos seleccionados para acciones en masa. */
  public readonly selectedIds = signal<Set<string>>(new Set<string>());

  /** Indica si una acción en masa está en curso. */
  public readonly bulkSaving = signal<boolean>(false);

  /** Búsqueda de tiendas. */
  public storeSearch = '';

  /** Orden de tiendas. */
  public storeSort: 'name' | 'products' = 'name';

  /** Página actual del listado de tiendas. */
  private storePageIndex = 0;

  /** Tamaño de página del listado de tiendas. */
  private readonly storePageSize = 12;

  /** Categorías disponibles para asignar al producto. */
  public readonly categories = signal<AdminCategory[]>([]);

  /** Texto de búsqueda del listado global. */
  public search = '';

  /** Filtro por visibilidad (cadena vacía = todas). */
  public activeFilter: '' | 'true' | 'false' = '';

  /** Página actual del listado global (empezando en 0). */
  private currentPage = 0;

  /** Tamaño de página del listado global. */
  private readonly pageSize = 10;

  /** Indica si el formulario de creación/edición está visible. */
  public readonly formOpen = signal<boolean>(false);

  /** Identificador del producto en edición (nulo si se está creando). */
  public readonly editingId = signal<string | null>(null);

  /** Indica si se está guardando. */
  public readonly saving = signal<boolean>(false);

  /** Mensaje de error del formulario. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de confirmación de acciones en masa. */
  public readonly okMessage = signal<string | null>(null);

  /** Imágenes que se están editando. */
  public readonly images = signal<ProductImage[]>([]);

  /** Variantes del producto en edición. */
  public readonly variants = signal<ProductVariant[]>([]);

  /** Formulario con los datos principales del producto. */
  public readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    slug: ['', [Validators.required, Validators.maxLength(220)]],
    description: ['', [Validators.required, Validators.maxLength(500)]],
    longDescription: [''],
    price: [0, [Validators.required, Validators.min(0)]],
    originalPrice: [null as number | null],
    currency: [CURRENCY_CODE],
    stock: [0, [Validators.required, Validators.min(0)]],
    rating: [null as number | null],
    reviewCount: [null as number | null],
    categoriesText: [''],
    tagsText: [''],
    isNew: [false],
    isFeatured: [false],
    onSale: [false],
    requiresShipping: [false],
    shippingCost: [0, [Validators.min(0)]],
    freeShippingThreshold: [null as number | null, [Validators.min(0)]]
  });

  /**
   * Carga el catálogo global y las categorías al abrir el panel.
   */
  constructor() {
    this.loadProductStores();
    this.adminService.listCategories(true).subscribe({
      next: categories => this.categories.set(categories),
      error: () => this.categories.set([])
    });
  }

  /**
   * Carga las tiendas con sus contadores agregados.
   */
  public loadProductStores(): void {
    this.storeLoading.set(true);
    this.adminService.listProductStores(
      this.storeSearch,
      this.storeSort,
      this.storePageIndex,
      this.storePageSize
    ).subscribe({
      next: page => {
        this.storePage.set(page);
        this.storeLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudieron cargar las tiendas.'
        );
        this.storeLoading.set(false);
      }
    });
  }

  /**
   * Busca tiendas desde la primera página.
   */
  public reloadStores(): void {
    this.storePageIndex = 0;
    this.loadProductStores();
  }

  /**
   * Cambia la página del listado de tiendas.
   *
   * @param delta desplazamiento (-1 o 1).
   */
  public changeStorePage(delta: number): void {
    const total = this.storePage()?.totalPages ?? 0;
    const next = this.storePageIndex + delta;
    if (next < 0 || next >= total) return;
    this.storePageIndex = next;
    this.loadProductStores();
  }

  /**
   * Selecciona una tienda y consulta solo su catálogo.
   *
   * @param store tienda seleccionada.
   */
  public selectStore(store: AdminStoreProductSummary): void {
    this.selectedStore.set(store);
    this.currentPage = 0;
    this.search = '';
    this.activeFilter = '';
    this.loadAdminProducts();
  }

  /**
   * Vuelve al selector de tiendas.
   */
  public backToStores(): void {
    this.selectedStore.set(null);
    this.adminPage.set(null);
    this.formOpen.set(false);
    this.editingId.set(null);
    this.loadProductStores();
  }

  /**
   * Carga la página del catálogo global con los filtros actuales.
   *
   * @returns {void}
   */
  public loadAdminProducts(): void {
    this.adminLoading.set(true);
    this.errorMessage.set(null);

    this.adminService.listProducts({
      search: this.search,
      sellerId: this.selectedStore()
        ? (this.selectedStore()!.storeId === null ? -1 : this.selectedStore()!.sellerId)
        : null,
      active: this.activeFilter === '' ? null : this.activeFilter === 'true',
      page: this.currentPage,
      size: this.pageSize
    }).subscribe({
      next: page => {
        this.adminPage.set(page);
        this.selectedIds.set(new Set());
        this.adminLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cargar el catálogo global.'
        );
        this.adminLoading.set(false);
      }
    });
  }

  /**
   * Recarga el catálogo global desde la primera página.
   *
   * @returns {void}
   */
  public reloadAdmin(): void {
    this.currentPage = 0;
    this.loadAdminProducts();
  }

  /**
   * Avanza o retrocede de página en el listado global.
   *
   * @param delta desplazamiento de páginas (-1 o 1).
   * @returns {void}
   */
  public changePage(delta: number): void {
    const total = this.adminPage()?.totalPages ?? 0;
    const next = this.currentPage + delta;
    if (next < 0 || next >= total) return;

    this.currentPage = next;
    this.loadAdminProducts();
  }

  /**
   * Publica u oculta un producto del catálogo.
   *
   * @param product producto a moderar.
   * @returns {void}
   */
  public toggleVisibility(product: AdminProduct): void {
    const hide = product.active;
    const confirmed = window.confirm(
      hide
        ? `¿Ocultar el producto "${product.name}" del catálogo público? Se avisará al vendedor.`
        : `¿Publicar de nuevo el producto "${product.name}"?`
    );
    if (!confirmed) return;

    this.errorMessage.set(null);

    this.adminService.changeProductVisibility(product.id, !product.active).subscribe({
      next: () => {
        this.loadAdminProducts();
        this.loadProductStores();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cambiar la visibilidad del producto.'
        );
      }
    });
  }

  /**
   * Elimina un producto del catálogo global tras pedir confirmación.
   *
   * @param product producto a eliminar.
   * @returns {void}
   */
  public deleteAdminProduct(product: AdminProduct): void {
    const confirmed = window.confirm(`¿Eliminar el producto "${product.name}"?`);
    if (!confirmed) return;

    this.errorMessage.set(null);

    this.adminService.deleteProduct(product.id).subscribe({
      next: () => {
        this.loadAdminProducts();
        this.loadProductStores();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo eliminar el producto.'
        );
      }
    });
  }

  /**
   * Añade o quita un producto de la selección.
   *
   * @param id identificador del producto.
   */
  public toggleSelection(id: string): void {
    this.selectedIds.update(current => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  /** Indica si un producto está seleccionado. */
  public isSelected(id: string): boolean {
    return this.selectedIds().has(id);
  }

  /** Indica si todos los productos visibles están seleccionados. */
  public allSelected(): boolean {
    const content = this.adminPage()?.content ?? [];
    return content.length > 0 && content.every(product => this.selectedIds().has(product.id));
  }

  /** Limpia la selección. */
  public clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  /** Selecciona o deselecciona los productos visibles. */
  public toggleAll(): void {
    if (this.allSelected()) {
      this.selectedIds.set(new Set());
      return;
    }
    this.selectedIds.set(new Set((this.adminPage()?.content ?? []).map(product => product.id)));
  }

  /**
   * Publica u oculta los productos seleccionados.
   *
   * @param active visibilidad deseada.
   */
  public bulkVisibility(active: boolean): void {
    const ids = [...this.selectedIds()];
    if (ids.length === 0) return;
    const action = active ? 'publicar' : 'ocultar';
    if (!window.confirm(`¿${action.charAt(0).toUpperCase() + action.slice(1)} ${ids.length} producto(s)?`)) {
      return;
    }

    this.bulkSaving.set(true);
    this.errorMessage.set(null);
    this.adminService.bulkProductVisibility(ids, active).subscribe({
      next: result => {
        this.bulkSaving.set(false);
        this.okMessage.set(
          `${result.updated} producto(s) actualizado(s); ${result.skipped} sin cambios.`
        );
        this.selectedIds.set(new Set());
        this.loadAdminProducts();
        this.loadProductStores();
      },
      error: (error: HttpErrorResponse) => {
        this.bulkSaving.set(false);
        this.errorMessage.set(
          error.error?.message || 'No se pudieron actualizar los productos seleccionados.'
        );
      }
    });
  }

  /** Elimina en masa los productos seleccionados. */
  public bulkDelete(): void {
    const ids = [...this.selectedIds()];
    if (ids.length === 0) return;
    if (!window.confirm(`¿Eliminar ${ids.length} producto(s) seleccionado(s)?`)) {
      return;
    }

    this.bulkSaving.set(true);
    this.errorMessage.set(null);
    this.adminService.bulkDeleteProducts(ids).subscribe({
      next: result => {
        this.bulkSaving.set(false);
        this.okMessage.set(
          `${result.updated} producto(s) eliminado(s).`
        );
        this.selectedIds.set(new Set());
        this.loadAdminProducts();
        this.loadProductStores();
      },
      error: (error: HttpErrorResponse) => {
        this.bulkSaving.set(false);
        this.errorMessage.set(
          error.error?.message || 'No se pudieron eliminar los productos seleccionados.'
        );
      }
    });
  }

  /**
   * Devuelve el nombre del propietario del producto.
   *
   * @param product producto del catálogo global.
   * @returns el nombre del vendedor o la etiqueta de la plataforma.
   */
  public ownerLabel(product: AdminProduct): string {
    return product.storeName || product.sellerName || 'ParDrops';
  }

  /**
   * Abre el formulario de edición de un producto del catálogo global.
   *
   * <p>El formulario trabaja con el producto completo (imágenes, variantes y
   * descripción larga), que el panel carga del catálogo público. Si el producto
   * está oculto no forma parte de ese catálogo, así que se pide publicarlo
   * antes de editarlo para no guardar una versión incompleta.</p>
   *
   * @param adminProduct producto del listado global.
   * @returns {void}
   */
  public editFromAdmin(adminProduct: AdminProduct): void {
    this.adminService.getFullProduct(adminProduct.id).subscribe({
      next: product => this.openEdit(product),
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cargar el producto para editarlo.'
        );
      }
    });
  }

  /**
   * Devuelve la etiqueta legible del estado de una tienda.
   *
   * @param status estado de la tienda.
   * @returns la etiqueta en español.
   */
  public storeStatusLabel(status: AdminStoreStatus | null): string {
    switch (status) {
      case 'ACTIVE': return 'Tienda activa';
      case 'SUSPENDED': return 'Tienda suspendida';
      case 'PENDING': return 'Tienda pendiente';
      default: return 'Sin tienda';
    }
  }

  /**
   * Devuelve la etiqueta legible del estado de una cuenta de vendedor.
   *
   * @param status estado de la cuenta.
   * @returns la etiqueta en español.
   */
  public sellerStatusLabel(status: AdminUserStatus | null): string {
    if (status === 'SUSPENDED') return 'Cuenta suspendida';
    if (status === 'ACTIVE') return 'Cuenta activa';
    return '';
  }

  /**
   * Abre el formulario para crear un producto nuevo.
   *
   * @returns {void}
   */
  public openCreate(): void {
    this.editingId.set(null);
    this.images.set([{ url: '', alt: '', isPrimary: true }]);
    this.variants.set([]);
    this.form.reset({
      name: '', slug: '', description: '', longDescription: '',
      price: 0, originalPrice: null, currency: CURRENCY_CODE, stock: 0,
      rating: null, reviewCount: null, categoriesText: '', tagsText: '',
      isNew: false, isFeatured: false, onSale: false,
      requiresShipping: false, shippingCost: 0, freeShippingThreshold: null
    });
    this.errorMessage.set(null);
    this.formOpen.set(true);
  }

  /**
   * Abre el formulario cargado con los datos de un producto existente.
   *
   * @param product producto a editar.
   * @returns {void}
   */
  public openEdit(product: Product): void {
    this.editingId.set(product.id);
    this.variants.set(product.variants ?? []);
    this.images.set(
      product.images.length > 0
        ? product.images.map(image => ({ ...image }))
        : [{ url: '', alt: '', isPrimary: true }]
    );
    this.form.reset({
      name: product.name,
      slug: product.slug,
      description: product.description,
      longDescription: product.longDescription ?? '',
      price: product.price,
      originalPrice: product.originalPrice ?? null,
      currency: product.currency,
      stock: product.stock,
      rating: product.rating ?? null,
      reviewCount: product.reviewCount ?? null,
      categoriesText: product.categories.join(', '),
      tagsText: (product.tags ?? []).join(', '),
      isNew: product.isNew,
      isFeatured: product.isFeatured,
      onSale: product.onSale,
      requiresShipping: product.requiresShipping === true,
      shippingCost: product.shippingCost ?? 0,
      freeShippingThreshold: product.freeShippingThreshold ?? null
    });
    this.errorMessage.set(null);
    this.formOpen.set(true);
  }

  /** Cierra el formulario sin guardar. */
  public cancel(): void {
    this.formOpen.set(false);
    this.editingId.set(null);
    this.errorMessage.set(null);
  }

  /**
   * Genera un slug a partir del nombre del producto.
   *
   * @returns {void}
   */
  public generateSlug(): void {
    const name = this.form.controls.name.value ?? '';
    this.form.controls.slug.setValue(this.slugify(name));
  }

  /** Añade una imagen vacía a la lista de imágenes. */
  public addImage(): void {
    this.images.update(list => [...list, { url: '', alt: '', isPrimary: list.length === 0 }]);
  }

  /**
   * Añade a la lista una imagen subida al servidor.
   *
   * @param url URL pública devuelta por el servicio de multimedia.
   * @returns {void}
   */
  public addUploadedImage(url: string): void {
    const cleanUrl = url.trim();
    if (!cleanUrl) {
      return;
    }

    this.images.update(list => {
      // Si solo había un hueco vacío, la imagen subida lo ocupa.
      if (list.length === 1 && list[0].url.trim() === '') {
        return [{
          url: cleanUrl,
          alt: (this.form.controls.name.value ?? '').trim(),
          isPrimary: true
        }];
      }

      return [
        ...list,
        {
          url: cleanUrl,
          alt: (this.form.controls.name.value ?? '').trim(),
          isPrimary: list.length === 0
        }
      ];
    });
  }

  /**
   * Elimina una imagen de la lista.
   *
   * @param index posición de la imagen.
   */
  public removeImage(index: number): void {
    this.images.update(list => list.filter((_, i) => i !== index));
  }

  /**
   * Actualiza la URL de una imagen.
   *
   * @param index posición de la imagen.
   * @param url   nueva URL.
   */
  public updateImageUrl(index: number, url: string): void {
    this.images.update(list => list.map((image, i) => (i === index ? { ...image, url } : image)));
  }

  /**
   * Actualiza el texto alternativo de una imagen.
   *
   * @param index posición de la imagen.
   * @param alt   nuevo texto alternativo.
   */
  public updateImageAlt(index: number, alt: string): void {
    this.images.update(list => list.map((image, i) => (i === index ? { ...image, alt } : image)));
  }

  /**
   * Marca una imagen como principal (única).
   *
   * @param index posición de la imagen principal.
   */
  public setPrimaryImage(index: number): void {
    this.images.update(list => list.map((image, i) => ({ ...image, isPrimary: i === index })));
  }

  /**
   * Guarda el producto (lo crea o lo actualiza según el modo).
   *
   * @returns {void}
   */
  public save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const validImages = this.images().filter(image => image.url.trim().length > 0);

    if (validImages.length === 0) {
      this.errorMessage.set('Añade al menos una imagen con una URL válida.');
      return;
    }

    const raw = this.form.getRawValue();

    if (raw.requiresShipping && Number(raw.shippingCost) <= 0) {
      this.errorMessage.set('Indica el costo de envío o desmarca que el producto requiere envío.');
      return;
    }

    const request: ProductRequest = {
      name: (raw.name ?? '').trim(),
      slug: (raw.slug ?? '').trim(),
      description: (raw.description ?? '').trim(),
      longDescription: raw.longDescription?.trim() || null,
      price: Number(raw.price),
      originalPrice: raw.originalPrice === null ? null : Number(raw.originalPrice),
      currency: raw.currency || CURRENCY_CODE,
      stock: Number(raw.stock),
      variants: this.variants(),
      rating: raw.rating === null ? null : Number(raw.rating),
      reviewCount: raw.reviewCount === null ? null : Number(raw.reviewCount),
      categories: this.parseList(raw.categoriesText),
      tags: this.parseList(raw.tagsText),
      images: validImages.map(image => ({
        url: image.url.trim(),
        alt: image.alt?.trim() || (raw.name ?? '').trim(),
        isPrimary: image.isPrimary
      })),
      isNew: raw.isNew ?? false,
      isFeatured: raw.isFeatured ?? false,
      onSale: raw.onSale ?? false,
      requiresShipping: raw.requiresShipping ?? false,
      shippingCost: raw.requiresShipping ? Number(raw.shippingCost) : null,
      freeShippingThreshold: raw.requiresShipping
        ? (raw.freeShippingThreshold === null ? null : Number(raw.freeShippingThreshold))
        : null
    };

    this.saving.set(true);
    this.errorMessage.set(null);

    const id = this.editingId();
    const operation = id
      ? this.adminService.updateProduct(id, request)
      : this.adminService.createProduct(request);

    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.cancel();
        this.loadAdminProducts();
        this.loadProductStores();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo guardar el producto.'
        );
        this.saving.set(false);
      }
    });
  }

  /**
   * Elimina un producto tras pedir confirmación.
   *
   * @param product producto a eliminar.
   * @returns {void}
   */
  public delete(product: Product): void {
    const confirmed = window.confirm(`¿Eliminar el producto "${product.name}"?`);
    if (!confirmed) return;

    this.adminService.deleteProduct(product.id).subscribe({
      next: () => {
        this.loadAdminProducts();
        this.loadProductStores();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(error.error?.message || 'No se pudo eliminar el producto.');
      }
    });
  }

  /**
   * Obtiene la imagen principal de un producto para la vista de tabla.
   *
   * @param product producto.
   * @returns URL de la imagen principal o la primera disponible.
   */
  public primaryImage(product: Product): string {
    return product.images.find(image => image.isPrimary)?.url
      || product.images[0]?.url
      || '';
  }

  /**
   * Formatea un importe como moneda.
   *
   * @param price importe a formatear.
   * @returns el importe formateado en pesos colombianos.
   */
  public formatPrice(price: number): string {
    return formatCurrency(price);
  }

  /**
   * Convierte un texto separado por comas en una lista limpia.
   *
   * @param text texto a dividir.
   * @returns lista de valores sin espacios vacíos.
   * @private
   */
  private parseList(text: string | null): string[] {
    if (!text) return [];
    return text
      .split(',')
      .map(item => item.trim())
      .filter(item => item.length > 0);
  }

  /**
   * Convierte un texto en un slug válido para URL.
   *
   * @param text texto de origen.
   * @returns slug normalizado.
   * @private
   */
  private slugify(text: string): string {
    return text
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
  }
}
