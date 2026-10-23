/**
 * @file src/app/pages/admin-categories/admin-categories.component.ts
 * @description Sección "Categorías" del panel de administración.
 *
 * Permite crear, editar y desactivar categorías del catálogo, y borrar las que
 * no estén en uso. Consume `GET/POST/PUT/DELETE /api/admin/categories`.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminService } from '../../core/services/admin.service';
import {
  AdminCategory,
  CategoryRequest
} from '../../core/models/admin.model';
import { formatDate as formatDateCO } from '../../core/config/commerce.config';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-categories.component.html',
  styleUrls: ['./admin-categories.component.scss']
})
export class AdminCategoriesComponent {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);

  /** Listado de categorías. */
  public readonly categories = signal<AdminCategory[]>([]);

  /** Indica si el listado está cargándose. */
  public readonly loading = signal<boolean>(true);

  /** Indica si el formulario está visible. */
  public readonly formOpen = signal<boolean>(false);

  /** Identificador de la categoría en edición (nulo al crear). */
  public readonly editingId = signal<number | null>(null);

  /** Indica si se está guardando. */
  public readonly saving = signal<boolean>(false);

  /** Mensaje de error visible. */
  public readonly errorMessage = signal<string | null>(null);

  /** Mensaje de confirmación visible. */
  public readonly okMessage = signal<string | null>(null);

  /** Formulario de la categoría. */
  public readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    slug: ['', [Validators.maxLength(140)]],
    description: ['', [Validators.maxLength(500)]],
    active: [true],
    featured: [false]
  });

  constructor() {
    this.load();
  }

  /**
   * Carga (o recarga) el listado de categorías.
   *
   * @returns {void}
   */
  public load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.adminService.listCategories(false).subscribe({
      next: categories => {
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudieron cargar las categorías.'
        );
        this.loading.set(false);
      }
    });
  }

  /** Abre el formulario para crear una categoría. */
  public openCreate(): void {
    this.editingId.set(null);
    this.form.reset({
      name: '', slug: '', description: '', active: true, featured: false
    });
    this.formOpen.set(true);
  }

  /**
   * Abre el formulario con los datos de una categoría existente.
   *
   * @param category categoría a editar.
   * @returns {void}
   */
  public openEdit(category: AdminCategory): void {
    this.editingId.set(category.id);
    this.form.reset({
      name: category.name,
      slug: category.slug,
      description: category.description ?? '',
      active: category.active,
      featured: category.featured
    });
    this.formOpen.set(true);
  }

  /** Cierra el formulario sin guardar. */
  public cancel(): void {
    this.formOpen.set(false);
    this.editingId.set(null);
  }

  /**
   * Genera un slug a partir del nombre.
   *
   * @returns {void}
   */
  public generateSlug(): void {
    const name = this.form.controls.name.value ?? '';
    this.form.controls.slug.setValue(this.slugify(name));
  }

  /**
   * Guarda la categoría (la crea o la actualiza según el modo).
   *
   * @returns {void}
   */
  public save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request: CategoryRequest = {
      name: (raw.name ?? '').trim(),
      slug: (raw.slug ?? '').trim() || null,
      description: (raw.description ?? '').trim() || null,
      active: raw.active ?? true,
      featured: raw.featured ?? false
    };

    this.saving.set(true);
    this.errorMessage.set(null);
    this.okMessage.set(null);

    const id = this.editingId();
    const operation = id
      ? this.adminService.updateCategory(id, request)
      : this.adminService.createCategory(request);

    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.okMessage.set(id ? 'Categoría actualizada.' : 'Categoría creada.');
        this.cancel();
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo guardar la categoría.'
        );
        this.saving.set(false);
      }
    });
  }

  /**
   * Activa o desactiva una categoría.
   *
   * @param category categoría a modificar.
   * @returns {void}
   */
  public toggleActive(category: AdminCategory): void {
    const request: CategoryRequest = {
      name: category.name,
      slug: category.slug,
      description: category.description,
      active: !category.active,
      featured: category.featured
    };

    this.errorMessage.set(null);
    this.okMessage.set(null);

    this.adminService.updateCategory(category.id, request).subscribe({
      next: () => {
        this.okMessage.set(
          `Categoría ${category.active ? 'desactivada' : 'activada'}.`
        );
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo cambiar el estado de la categoría.'
        );
      }
    });
  }

  /**
   * Elimina una categoría tras pedir confirmación.
   *
   * @param category categoría a eliminar.
   * @returns {void}
   */
  public remove(category: AdminCategory): void {
    const confirmed = window.confirm(
      `¿Eliminar la categoría "${category.name}"?`
    );
    if (!confirmed) return;

    this.errorMessage.set(null);
    this.okMessage.set(null);

    this.adminService.deleteCategory(category.id).subscribe({
      next: () => {
        this.okMessage.set('Categoría eliminada.');
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message
            || 'No se pudo eliminar la categoría (puede estar en uso).'
        );
      }
    });
  }

  /**
   * Formatea una fecha en formato ISO.
   *
   * @param isoDate fecha de origen.
   * @returns la fecha formateada en español.
   */
  public formatDate(isoDate: string): string {
    return formatDateCO(isoDate);
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
