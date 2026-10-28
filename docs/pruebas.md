# Pruebas

## Resumen de ejecución

### Backend (Maven)

```powershell
$env:JAVA_HOME = (Resolve-Path '.\.tools\jdk25').Path
cd pardrops
.\mvnw.cmd -o test
```

**Resultado:** `Tests run: 235, Failures: 0, Errors: 0, Skipped: 0` · `BUILD SUCCESS`.

### Frontend (Vitest)

```bash
cd pardropsfrontend
npx ng test --watch=false
```

**Resultado:** `Test Files: 18 passed (18)` · `Tests: 87 passed (87)`.

### Compilación

```bash
npm run build
```

**Resultado:** compilación correcta con los warnings preexistentes de presupuesto SCSS.

## Comisiones y saldo

| Clase | Casos |
|---|---|
| `SellerBalanceTest` | Valores iniciales y actualización de importes. |
| `SellerBalanceServiceTest` | Retención, liberación, cancelación, idempotencia y subpedidos de plataforma. |
| `FinanceSchemaMigrationTest` | Columnas financieras y migración idempotente. |
| `OrderServiceTest` | Comisión del 5%, neto, estado `HELD`, retención y variantes. |
| `SellerOrderServiceTest` | Liberación al entregar y anulación al cancelar. |
| `AdminUserServiceTest` / `SellerDashboardServiceTest` | Saldos y comisiones en los paneles. |

## Tienda y privacidad

| Clase | Casos |
|---|---|
| `StoreServiceTest` | Datos públicos, rating y reseñas, tienda suspendida. |
| `StorePrivacySchemaMigrationTest` | Columnas nuevas e idempotencia. |
| `SellerApplicationServiceTest` | Datos públicos y legales de la solicitud. |
| `SecurityPermissionsTest` | Catálogo público y paneles protegidos. |

## Variantes

| Clase | Casos |
|---|---|
| `ProductServiceTest` | Conservación de variantes al editar, categorías y rankings. |
| `OrderServiceTest` | Precio y stock por variante, variante agotada y comisión. |

## Reseñas con imágenes y moderación

| Clase o archivo | Casos |
|---|---|
| `ProductReviewServiceTest` | Creación, valoración media y borrado por el administrador con auditoría. |
| `AdminReviewController` | Listado y borrado bajo `/api/admin/reviews`. |
| `admin.service.spec.ts` | Listar y eliminar reseñas desde el panel. |
| `admin-reviews.component.spec.ts` | Carga y borrado con confirmación. |
| `product-detail` | Subida de hasta tres imágenes y galería. |
| `store-public` | Reseñas e imágenes en el perfil de tienda. |

## Portada

| Clase | Casos |
|---|---|
| `ProductServiceTest` | Más vendidos por unidades y mejor valorados por estrellas. |
| `ProductController` | Endpoint público `/api/products/highlights`. |

## Notas

- Las pruebas no necesitan SMTP ni credenciales reales.
- Las variantes, comisiones y reseñas se validan en el servidor; la interfaz solo refleja el resultado.
- No hay suite E2E con Playwright; el recorrido principal se verifica manualmente.
