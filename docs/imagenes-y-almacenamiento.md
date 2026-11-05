# Imágenes y almacenamiento

## Almacenamiento

- **Metadatos**: `MediaAsset` en base de datos (propietario, nombre original/almacenado, ruta pública, MIME, tamaño, fecha).
- **Ficheros**: en desarrollo se guardan en disco (`app.media.storage-dir`, por defecto `./data/media`). En producción se mantiene PostgreSQL como respaldo si no hay almacenamiento externo.
- **R2/CDN (opcional)**: con `APP_MEDIA_STORAGE=r2` y las variables `R2_*`, los ficheros se guardan en Cloudflare R2 y se sirven por el dominio CDN configurado. Las credenciales S3 de R2 son distintas del API Token de Cloudflare.
- **Migración**: `APP_MEDIA_MIGRATE_TO_R2=true` copia los BLOB históricos a R2 y actualiza referencias sin borrar los originales.
- **Servicio**: `MediaService` + `MediaStorage` gestionan guardado, validación y borrado.
- **Publicación**: `WebConfig` mapea `app.media.public-path` (`/media`) a la ubicación física; en producción `MediaFileController` sirve el contenido con ETag y caché inmutable.

## Validación

Configurado en `application.properties`:

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `app.media.max-size-bytes` | `5242880` (5 MB) | Tamaño máximo por imagen. |
| `app.media.allowed-types` | `image/jpeg,image/png,image/webp,image/gif` | Tipos MIME permitidos. |
| `spring.servlet.multipart.max-file-size` | `6MB` | Límite Multipart de Spring. |
| `spring.servlet.multipart.max-request-size` | `8MB` | Límite total de petición. |

**Reglas**:
- El tipo se valida por contenido/tipo MIME admitido.
- El usuario solo puede gestionar sus propias imágenes (`canManage`).
- Las imágenes públicas servidas por `/media/**` son accesibles sin autenticación.

## API

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/media` | Autenticado | Subir imagen (`multipart/form-data`, campo `file`). |
| `GET` | `/api/media` | Autenticado | Listar imágenes propias. |
| `GET` | `/api/media/{id}` | Autenticado (propietario o con permisos) | Consultar metadatos. |
| `DELETE` | `/api/media/{id}` | Autenticado (propietario) | Eliminar imagen y fichero. |

## Uso en el frontend

- **Componente**: `ImageUploaderComponent` (compartido). Permite seleccionar archivo, previsualizar, subir y emitir la URL pública.
- **Panel vendedor**: `Mi tienda` permite subir logotipo y portada; `Mis productos` permite añadir múltiples imágenes al producto.
- **Panel admin**: gestión de productos/imágenes según flujos existentes.
- **Modelos**: `MediaAsset` con `url`, `path`, `mimeType`, etc.

## Buenas prácticas

- **No commitear medios**: `data/` está en `.gitignore`. Las imágenes subidas son locales.
- **URLs relativas/absolutas**: el backend construye la URL pública con el contexto de la petición.
- **Limpieza**: al eliminar el `MediaAsset` se borra el fichero físico si es gestionable.
- **Seguridad**: validación de tipo/tamaño; solo propietario puede borrar.
