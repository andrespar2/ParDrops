/**
 * @file src/app/shared/components/image-uploader/image-uploader.component.ts
 * @description Componente reutilizable para subir y previsualizar imagenes locales.
 *
 * Permite seleccionar un fichero desde el dispositivo, previsualizarlo, subirlo
 * al servidor mediante el servicio de multimedia y devolver la URL publica
 * para asociarla a un producto.
 */

import { Component, EventEmitter, inject, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MediaService } from '../../../core/services/media.service';
import { MediaAsset } from '../../../core/models/media.model';

@Component({
  selector: 'app-image-uploader',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './image-uploader.component.html',
  styleUrls: ['./image-uploader.component.scss']
})
export class ImageUploaderComponent {
  private readonly mediaService = inject(MediaService);

  /** Emite la URL publica de la imagen subida. */
  @Output() public imageUploaded = new EventEmitter<string>();

  /** Imagen seleccionada para previsualizar (data URL). */
  public readonly preview = signal<string | null>(null);

  /** Fichero seleccionado. */
  public readonly selectedFile = signal<File | null>(null);

  /** Indica si se esta subiendo la imagen. */
  public readonly uploading = signal<boolean>(false);

  /** Mensaje de error. */
  public readonly errorMessage = signal<string | null>(null);

  /** Formatos y tamano permitidos (mensaje informativo). */
  public readonly allowedHint = 'Formatos admitidos: JPG, PNG, WEBP, GIF. Tamano maximo: 5 MB.';

  /**
   * Gestiona la seleccion de un fichero.
   *
   * @param event evento del input de tipo file.
   * @returns {void}
   */
  public onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';

    if (!file) {
      return;
    }

    this.errorMessage.set(null);
    this.selectedFile.set(file);

    const reader = new FileReader();
    reader.onload = () => {
      this.preview.set(reader.result as string);
    };
    reader.readAsDataURL(file);
  }

  /**
   * Sube la imagen seleccionada al servidor.
   *
   * @returns {void}
   */
  public upload(): void {
    const file = this.selectedFile();
    if (!file) {
      return;
    }

    this.uploading.set(true);
    this.errorMessage.set(null);

    this.mediaService.upload(file).subscribe({
      next: (asset: MediaAsset) => {
        this.uploading.set(false);
        this.imageUploaded.emit(asset.url);
        this.clear();
      },
      error: (error: HttpErrorResponse) => {
        this.uploading.set(false);
        this.errorMessage.set(error.error?.message || 'No se pudo subir la imagen.');
      }
    });
  }

  /**
   * Cancela la seleccion y limpia la previsualizacion.
   *
   * @returns {void}
   */
  public clear(): void {
    this.preview.set(null);
    this.selectedFile.set(null);
    this.errorMessage.set(null);
  }
}
