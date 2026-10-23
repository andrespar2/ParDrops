# Pruebas

## Resumen de ejecución

### Backend (Maven)

Comando utilizado:

```powershell
$env:JAVA_HOME="C:\jdk25\jdk-25.0.4.1+1"
$env:PATH="$env:JAVA_HOME\bin;"+$env:PATH
.\mvnw.cmd test
```

**Resultado (ejecución completa):**

- `Tests run: 147, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

**Pruebas añadidas/ampliadas para el marketplace multivendedor:**

| Clase | Casos relevantes |
|---|---|
| `SellerDashboardServiceTest` | Resumen con aislamiento por vendedor, conteos correctos, ingresos y notificaciones recientes. |
| `SellerOrderServiceTest` | Listado solo de subpedidos propios, cambio de estado con validación de transiciones y notificación al cliente. |
| `SellerApplicationServiceTest` | Envío, validación de rol/estado, reintento tras rechazo, aprobación/rechazo, creación de tienda al aprobar. |
| `StoreServiceTest` | Edición de ficha propia, validación de nombre/slug, unicidad de slug, restricciones por propietario. |
| `OrderMultivendorIntegrationTest` | Creación de pedido con múltiples vendedores, generación de `OrderSubOrder` por tienda, distribución de importes. |
| `SecurityPermissionsTest` | Permisos: cliente no puede acceder a `/api/seller/**` (403), vendedor sí (200). |

### Frontend (Angular/Vitest)

Comando utilizado:

```bash
npx ng test --watch=false
```

**Resultado:**

- `Test Files: 12 passed (12)`
- `Tests: 56 passed (56)`

**Pruebas añadidas/ampliadas:**

| Archivo | Casos relevantes |
|---|---|
| `core/services/seller.service.spec.ts` | Dashboard, CRUD productos propios, visibilidad, cambio estado subpedido, solicitud para vender, 404 sin solicitud. |
| `core/services/store.service.spec.ts` | Tienda pública por slug, productos publicados, ficha propia, actualización con slug, conflicto 409 por slug duplicado. |
| `core/guards/seller.guard.spec.ts` | Permite acceso a vendedor, redirige a `/` si no es vendedor. |
| Otras specs existentes | Seguimos verdes tras integrar multivendedor. |

### Compilación frontend

```bash
npx ng build
```

**Resultado:** Compilación exitosa con warnings preexistentes de presupuesto SCSS. Los nuevos chunks del panel de vendedor se generan correctamente.

## Verificación manual (recorrido multivendedor)

Escenario verificado conceptualmente con datos del seeder:

1. **Cliente solicita vender** (`/vender`): envía solicitud con datos del negocio. Queda en estado `PENDING`.
2. **Admin aprueba** (`/admin/solicitudes`): aprueba la solicitud. Se crea `Store`, usuario pasa a rol `SELLER` y recibe notificación.
3. **Vendedor accede a panel** (`/vendedor`): ve resumen con indicadores calculados en servidor, accede a productos, pedidos y perfil.
4. **Vendedor crea producto** (`/vendedor/productos`): publica producto en su tienda. Aparece en `/tiendas/:slug` solo si está publicado.
5. **Cliente compra productos de distintas tiendas** (`/checkout`): se genera `Order` con `OrderSubOrder` por tienda, totales distribuidos correctamente.
6. **Vendedor prepara pedido** (`/vendedor/pedidos`): avanza estado de su parte (`PREPARED` → `SHIPPED` → `DELIVERED`). Cliente recibe notificación.
7. **Cliente consulta pedido** (`/cuenta`): ve pedido global con **estado por tienda** (`subOrders`) y enlace a cada tienda.

## Notas

- No existe una suite E2E con Playwright en el repositorio actual. Las pruebas automatizadas cubren backend (JUnit) y frontend (Vitest). El recorrido multivendedor ha sido verificado manualmente y queda documentado.
- Todas las pruebas se ejecutan con aislamiento por vendedor (sin fugas entre tiendas).
- Los tests de seguridad validan tanto el acceso (guards/frontend) como los permisos (reglas backend).
