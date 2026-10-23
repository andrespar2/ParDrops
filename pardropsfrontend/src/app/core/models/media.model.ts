/**
 * @file src/app/core/models/media.model.ts
 * @description Modelos relacionados con las imagenes subidas a la plataforma.
 */

export interface MediaAsset {
  id: number;
  url: string;
  path: string;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  createdAt: string;
}
