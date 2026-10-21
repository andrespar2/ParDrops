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
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ProductService } from '../../core/services/product.service';
import { ProductRequest } from '../../core/models/product-request.model';
import { Product, ProductImage } from '../../core/models/product.model';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-products.component.html',
  styleUrls: ['./admin-products.component.scss']
})
export class AdminProductsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly productService = inject(ProductService);

  /** Lista de productos (signal del servicio). */
  public readonly products = this.productService.products;

  /** Indica si el catálogo está cargándose. */
  public readonly loading = this.productService.loading;

  /** Indica si el formulario de creación/edición está visible. */
  public readonly formOpen = signal<boolean>(false);

  /** Identificador del producto en edición (nulo si se está creando). */
  public readonly editingId = signal<string | null>(null);

  /** Indica si se está guardando. */
  public readonly saving = signal<boolean>(false);

  /** Mensaje de error del formulario. */
  public readonly errorMessage = signal<string | null>(null);

  /** Imágenes que se están editando. */
  public readonly images = signal<ProductImage[]>([]);

  /** Formulario con los datos principales del producto. */
  public readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    slug: ['', [Validators.required, Validators.maxLength(220)]],
    description: ['', [Validators.required, Validators.maxLength(500)]],
    longDescription: [''],
    price: [0, [Validators.required, Validators.min(0)]],
    originalPrice: [null as number | null],
    currency: ['COP'],
    stock: [0, [Validators.required, Validators.min(0)]],
    rating: [null as number | null],
    reviewCount: [null as number | null],
    categoriesText: [''],
    tagsText: [''],
    isNew: [false],
    isFeatured: [false],
    onSale: [false]
  });

  /**
   * Abre el formulario para crear un producto nuevo.
   *
   * @returns {void}
   */
  public openCreate(): void {
    this.editingId.set(null);
    this.images.set([{ url: '', alt: '', isPrimary: true }]);
    this.form.reset({
      name: '', slug: '', description: '', longDescription: '',
      price: 0, originalPrice: null, currency: 'COP', stock: 0,
      rating: null, reviewCount: null, categoriesText: '', tagsText: '',
      isNew: false, isFeatured: false, onSale: false
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
      currency: product.currency || 'COP',
      stock: product.stock,
      rating: product.rating ?? null,
      reviewCount: product.reviewCount ?? null,
      categoriesText: product.categories.join(', '),
      tagsText: (product.tags ?? []).join(', '),
      isNew: product.isNew,
      isFeatured: product.isFeatured,
      onSale: product.onSale
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

    const request: ProductRequest = {
      name: (raw.name ?? '').trim(),
      slug: (raw.slug ?? '').trim(),
      description: (raw.description ?? '').trim(),
      longDescription: raw.longDescription?.trim() || null,
      price: Number(raw.price),
      originalPrice: raw.originalPrice === null ? null : Number(raw.originalPrice),
      currency: raw.currency || 'COP',
      stock: Number(raw.stock),
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
      onSale: raw.onSale ?? false
    };

    this.saving.set(true);
    this.errorMessage.set(null);

    const id = this.editingId();
    const operation = id
      ? this.productService.updateProduct(id, request)
      : this.productService.createProduct(request);

    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.cancel();
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

    this.productService.deleteProduct(product.id).subscribe({
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
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(price);
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
