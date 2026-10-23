# Imágenes y almacenamiento

## Almacenamiento

- **Metadatos**: `MediaAsset` en base de datos (propietario, nombre original/almacenado, ruta pública, MIME, tamaño, fecha).
- **Ficheros**: guardados en disco en `app.media.storage-dir` (por defecto `./data/media`). La carpeta `data/` está excluida del control de versiones.
- **Servicio**: `MediaService` + `FileSystemMediaStorage` gestionan guardado, validación y borrado.
- **Publicación**: `WebConfig` mapea `app.media.public-path` (`/media`) a la ubicación física para servir imágenes estáticamente.

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
