/**
 * @file src/app/pages/seller-profile/seller-profile.component.ts
 * @description Edición de la ficha pública de la tienda del vendedor.
 *
 * Permite cambiar el nombre comercial, el slug, la descripción, los datos de
 * contacto y las imágenes de la tienda. El backend valida que el slug sea
 * único y que el vendedor solo edite su propia ficha.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { SellerService } from '../../core/services/seller.service';
import { StoreService } from '../../core/services/store.service';
import { Store, StoreRequest } from '../../core/models/store.model';
import { ImageUploaderComponent } from '../../shared/components/image-uploader/image-uploader.component';

@Component({
  selector: 'app-seller-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, ImageUploaderComponent],
  templateUrl: './seller-profile.component.html',
  styleUrls: ['./seller-profile.component.scss']
})
export class SellerProfileComponent {
  private readonly storeService = inject(StoreService);
  private readonly sellerService = inject(SellerService);
  private readonly fb = inject(FormBuilder);

  /** Ficha actual de la tienda. */
  public readonly store = signal<Store | null>(null);

  /** Indica si la ficha está cargándose. */
  public readonly loading = signal<boolean>(true);

  /** Indica si se está guardando. */
  public readonly saving = signal<boolean>(false);

  /** Indica si se intentó guardar. */
  public readonly submitted = signal<boolean>(false);

  /** Mensaje de error. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de éxito. */
  public readonly successMessage = signal<string | null>(null);

  /** Formulario de la ficha comercial. */
  public readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    slug: ['', [
      Validators.required,
      Validators.maxLength(140),
      Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/)
    ]],
    description: [''],
    logoUrl: [''],
    bannerUrl: [''],
    contactEmail: ['', [Validators.email, Validators.maxLength(180)]],
    contactPhone: ['', [Validators.maxLength(30)]],
    whatsapp: ['', [Validators.maxLength(30)]]
  });

  constructor() {
    this.loadStore();
  }

  /**
   * Carga la ficha de la tienda del vendedor autenticado.
   *
   * @returns {void}
   */
  public loadStore(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.storeService.getMine().subscribe({
      next: store => {
        this.store.set(store);
        this.form.reset({
          name: store.name,
          slug: store.slug,
          description: store.description ?? '',
          logoUrl: store.logoUrl ?? '',
          bannerUrl: store.bannerUrl ?? '',
          contactEmail: store.contactEmail ?? '',
          contactPhone: store.contactPhone ?? '',
          whatsapp: store.whatsapp ?? ''
        });
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cargar la ficha de tu tienda.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Genera el slug a partir del nombre comercial.
   *
   * @returns {void}
   */
  public suggestSlug(): void {
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
   * Asigna la URL del logotipo subido.
   *
   * @param url URL pública.
   * @returns {void}
   */
  public setLogo(url: string): void {
    this.form.controls.logoUrl.setValue(url);
  }

  /**
   * Asigna la URL de la portada subida.
   *
   * @param url URL pública.
   * @returns {void}
   */
  public setBanner(url: string): void {
    this.form.controls.bannerUrl.setValue(url);
  }

  /**
   * Guarda la ficha comercial.
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
    const request: StoreRequest = {
      name: value.name.trim(),
      slug: value.slug.trim(),
      description: value.description.trim() || null,
      logoUrl: value.logoUrl.trim() || null,
      bannerUrl: value.bannerUrl.trim() || null,
      contactEmail: value.contactEmail.trim() || null,
      contactPhone: value.contactPhone.trim() || null,
      whatsapp: value.whatsapp.trim() || null
    };

    this.saving.set(true);
    this.storeService.updateMine(request).subscribe({
      next: store => {
        this.store.set(store);
        this.saving.set(false);
        this.successMessage.set('Ficha de la tienda actualizada.');
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo actualizar la ficha de la tienda.');
        this.saving.set(false);
      }
    });
  }

  /**
   * Abre la tienda pública en una pestaña nueva.
   *
   * @returns {void}
   */
  public openPublicStore(): void {
    const slug = this.store()?.slug;
    if (slug && typeof window !== 'undefined') {
      window.open(`/tiendas/${slug}`, '_blank');
    }
  }
}
