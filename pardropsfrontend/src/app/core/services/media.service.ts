/**
 * @file src/app/core/services/media.service.ts
 * @description Servicio de subida y gestion de imagenes locales.
 *
 * Permite subir una imagen desde el dispositivo al endpoint /api/media,
 * listar las imagenes subidas por el usuario y eliminarlas.
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { MediaAsset } from '../models/media.model';

@Injectable({
  providedIn: 'root'
})
export class MediaService {
  private readonly http = inject(HttpClient);
  private readonly mediaUrl = `${API_BASE_URL}/media`;

  /**
   * Sube una imagen al servidor.
   *
   * @param file fichero a subir.
   * @returns observable con la ficha de la imagen guardada.
   */
  public upload(file: File): Observable<MediaAsset> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<MediaAsset>(this.mediaUrl, formData);
  }

  /**
   * Lista las imagenes que ha subido el usuario autenticado.
   *
   * @returns observable con el listado de sus imagenes.
   */
  public listMine(): Observable<MediaAsset[]> {
    return this.http.get<MediaAsset[]>(this.mediaUrl);
  }

  /**
   * Elimina una imagen.
   *
   * @param id identificador de la imagen.
   * @returns observable sin contenido.
   */
  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.mediaUrl}/${id}`);
  }
}
