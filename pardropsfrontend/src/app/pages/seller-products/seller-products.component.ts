/**
 * @file src/app/pages/seller-products/seller-products.component.ts
 * @description Gestión del catálogo propio del vendedor.
 *
 * Permite crear, editar, publicar u ocultar y eliminar únicamente los
 * productos de la tienda autenticada. La pertenencia la valida el backend en
 * cada operación, por lo que la interfaz no puede modificar productos ajenos.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { SellerService } from '../../core/services/seller.service';
import { Product, ProductImage, ProductVariant } from '../../core/models/product.model';
import { ProductRequest } from '../../core/models/product-request.model';
import { ImageUploaderComponent } from '../../shared/components/image-uploader/image-uploader.component';
import { VariantEditorComponent } from '../../shared/components/variant-editor/variant-editor.component';
import { formatCurrency } from '../../core/config/commerce.config';

@Component({
  selector: 'app-seller-products',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ImageUploaderComponent, VariantEditorComponent],
  templateUrl: './seller-products.component.html',
  styleUrls: ['./seller-products.component.scss']
})
export class SellerProductsComponent {
  private readonly sellerService = inject(SellerService);
  private readonly fb = inject(FormBuilder);

  /** Productos del vendedor (publicados y ocultos). */
  public readonly products = signal<Product[]>([]);

  /** Indica si se está cargando la lista. */
  public readonly loading = signal<boolean>(true);

  /** Indica si hay una operación de guardado en curso. */
  public readonly saving = signal<boolean>(false);

  /** Indica si el formulario está visible. */
  public readonly formOpen = signal<boolean>(false);

  /** Identificador del producto en edición (nulo al crear). */
  public readonly editingId = signal<string | null>(null);

  /** Mensaje de error. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de éxito. */
  public readonly successMessage = signal<string | null>(null);

  /** Indica si se intentó guardar con errores de validación. */
  public readonly submitted = signal<boolean>(false);

  /** URLs de las imágenes del producto en edición. */
  public readonly imageUrls = signal<string[]>([]);

  /** Variantes del producto en edición. */
  public readonly variants = signal<ProductVariant[]>([]);

  /** Formulario de alta y edición. */
  public readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    slug: ['', [
      Validators.required,
      Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/)
    ]],
    description: ['', [Validators.required, Validators.maxLength(500)]],
    longDescription: [''],
    price: [0, [Validators.required, Validators.min(1)]],
    originalPrice: [null as number | null],
    stock: [0, [Validators.required, Validators.min(0)]],
    categories: [''],
    tags: [''],
    isNew: [false],
    isFeatured: [false],
    onSale: [false]
  });

  constructor() {
    this.loadProducts();
  }

  /**
   * Carga los productos del vendedor autenticado.
   *
   * @returns {void}
   */
  public loadProducts(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.sellerService.listProducts().subscribe({
      next: products => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudieron cargar tus productos.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Abre el formulario para crear un producto.
   *
   * @returns {void}
   */
  public startCreate(): void {
    this.editingId.set(null);
    this.imageUrls.set([]);
    this.variants.set([]);
    this.submitted.set(false);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.form.reset({
      name: '',
      slug: '',
      description: '',
      longDescription: '',
      price: 0,
      originalPrice: null,
      stock: 0,
      categories: '',
      tags: '',
      isNew: false,
      isFeatured: false,
      onSale: false
    });
    this.formOpen.set(true);
  }

  /**
   * Abre el formulario con los datos de un producto propio.
   *
   * @param product producto a editar.
   * @returns {void}
   */
  public startEdit(product: Product): void {
    this.editingId.set(product.id);
    this.imageUrls.set(product.images?.map(image => image.url) ?? []);
    this.variants.set(product.variants ?? []);
    this.submitted.set(false);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.form.reset({
      name: product.name,
      slug: product.slug,
      description: product.description,
      longDescription: product.longDescription ?? '',
      price: product.price,
      originalPrice: product.originalPrice ?? null,
      stock: product.stock,
      categories: (product.categories ?? []).join(', '),
      tags: (product.tags ?? []).join(', '),
      isNew: product.isNew,
      isFeatured: product.isFeatured,
      onSale: product.onSale
    });
    this.formOpen.set(true);
  }

  /**
   * Cierra el formulario sin guardar.
   *
   * @returns {void}
   */
  public cancelForm(): void {
    this.formOpen.set(false);
    this.editingId.set(null);
    this.imageUrls.set([]);
    this.variants.set([]);
  }

  /**
   * Genera un slug a partir del nombre (solo al crear).
   *
   * @returns {void}
   */
  public suggestSlug(): void {
    if (this.editingId()) {
      return;
    }
    const name = this.form.controls.name.value;
    const slug = name
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
    this.form.controls.slug.setValue(slug);
  }

  /**
   * Añade la URL de una imagen subida.
   *
   * @param url URL pública de la imagen.
   * @returns {void}
   */
  public addImage(url: string): void {
    if (!url || this.imageUrls().includes(url)) {
      return;
    }
    this.imageUrls.update(urls => [...urls, url]);
  }

  /**
   * Quita una imagen de la lista.
   *
   * @param index posición de la imagen.
   * @returns {void}
   */
  public removeImage(index: number): void {
    this.imageUrls.update(urls => urls.filter((_, i) => i !== index));
  }

  /**
   * Guarda el producto en edición o crea uno nuevo.
   *
   * @returns {void}
   */
  public save(): void {
    this.submitted.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const images: ProductImage[] = this.imageUrls().map((url, index) => ({
      url,
      alt: value.name,
      isPrimary: index === 0
    }));

    const request: ProductRequest = {
      name: value.name.trim(),
      slug: value.slug.trim(),
      description: value.description.trim(),
      longDescription: value.longDescription?.trim() || null,
      price: Number(value.price),
      originalPrice: value.originalPrice ? Number(value.originalPrice) : null,
      currency: 'COP',
      images,
      categories: this.splitList(value.categories),
      tags: this.splitList(value.tags),
      stock: Number(value.stock),
      variants: this.variants(),
      isNew: value.isNew,
      isFeatured: value.isFeatured,
      onSale: value.onSale
    };

    const editingId = this.editingId();
    this.saving.set(true);

    const operation = editingId
      ? this.sellerService.updateProduct(editingId, request)
      : this.sellerService.createProduct(request);

    operation.subscribe({
      next: () => {
        this.successMessage.set(
          editingId ? 'Producto actualizado.' : 'Producto creado.');
        this.saving.set(false);
        this.formOpen.set(false);
        this.editingId.set(null);
        this.loadProducts();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo guardar el producto.');
        this.saving.set(false);
      }
    });
  }

  /**
   * Publica u oculta un producto propio.
   *
   * @param product producto a cambiar.
   * @returns {void}
   */
  public toggleVisibility(product: Product): void {
    const active = product.active === false;
    this.errorMessage.set(null);
    this.sellerService.setProductVisibility(product.id, active).subscribe({
      next: updated => {
        this.products.update(list =>
          list.map(item => (item.id === updated.id ? updated : item)));
        this.successMessage.set(
          active ? 'Producto publicado.' : 'Producto oculto.');
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cambiar la visibilidad.');
      }
    });
  }

  /**
   * Elimina un producto propio tras confirmación.
   *
   * @param product producto a eliminar.
   * @returns {void}
   */
  public remove(product: Product): void {
    if (!window.confirm(`¿Eliminar "${product.name}" definitivamente?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.sellerService.deleteProduct(product.id).subscribe({
      next: () => {
        this.products.update(list => list.filter(item => item.id !== product.id));
        this.successMessage.set('Producto eliminado.');
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo eliminar el producto.');
      }
    });
  }

  /**
   * Formatea un precio.
   *
   * @param value importe.
   * @returns el importe formateado.
   */
  public formatPrice(value: number): string {
    return formatCurrency(value);
  }

  /**
   * Indica si el producto está publicado.
   *
   * @param product producto a comprobar.
   * @returns {@code true} si está visible en el catálogo.
   */
  public isPublished(product: Product): boolean {
    return product.active !== false;
  }

  /**
   * Convierte una lista separada por comas en un arreglo limpio.
   *
   * @param value texto con elementos separados por comas.
   * @returns los elementos sin espacios sobrantes.
   */
  private splitList(value: string): string[] {
    return value
      .split(',')
      .map(item => item.trim())
      .filter(item => item.length > 0);
  }
}
