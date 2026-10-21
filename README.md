<div align="center">

<h1 style="font-family: Georgia, 'Times New Roman', serif; font-size: 48px;">
  ParDrops
</h1>

<h3>Plataforma de Comercio Digital</h3>

<p>
  Una plataforma de comercio electrónico diseñada para ofrecer una experiencia
  de compra moderna, rápida e intuitiva.
</p>

<p>
  <a href="#objetivo">Objetivo</a> •
  <a href="#características">Características</a>
</p>

</div>

---

## Sobre el proyecto

**ParDrops** es una plataforma web de comercio electrónico que permite a los
usuarios explorar y descubrir productos, consultar información detallada y
gestionar su proceso de compra desde una interfaz atractiva y fácil de usar.

El proyecto está enfocado en proporcionar una experiencia fluida y organizada,
combinando **diseño responsive**, **navegación intuitiva** y funcionalidades
orientadas al comercio digital.

---

<details>
<summary><strong>✦ Objetivo</strong></summary>

<br>

Desarrollar una solución de comercio electrónico que conecte
**tecnología, diseño y accesibilidad** para simplificar la experiencia
de compra en línea.

ParDrops busca proporcionar un entorno donde los usuarios puedan descubrir
productos, consultar su información y realizar sus compras de manera
sencilla, rápida y eficiente.

</details>

<br>

<details>
<summary><strong>✦ Características</strong></summary>

<br>

* **◼ Exploración de productos**
  Permite descubrir y consultar los diferentes productos disponibles.

* **◻ Información detallada**
  Presenta información relevante de cada producto para facilitar la decisión de compra.

* **🛍 Carrito de compras**
  Permite gestionar los productos seleccionados antes de completar la compra.

* **✦ Proceso de compra**
  Ofrece un flujo organizado para gestionar el proceso de adquisición.

* **🔐 Cuentas de usuario**
  Registro e inicio de sesión con autenticación JWT. Los usuarios registrados
  conservan su historial de pedidos; los invitados pueden comprar sin historial.

* **⚙️ Panel de administración**
  Permite al administrador crear, editar y eliminar los productos del catálogo.
  Además avisa de las compras nuevas y desde él se consultan los datos de
  contacto y la dirección de envío del cliente, y se avanza el estado del pedido
  (preparado, enviado, entregado o cancelado).

* **★ Reseñas de productos**
  Los clientes que han comprado y recibido un producto pueden calificarlo de 1 a
  5 estrellas y dejar un comentario. Cada producto muestra su valoración media,
  el número de reseñas y la lista de opiniones; solo se admite una reseña por
  cliente y producto.

* **◼ Diseño responsive**
  Adaptado para ofrecer una experiencia adecuada en diferentes dispositivos.
  Adaptado para ofrecer una experiencia adecuada en diferentes dispositivos.

* **◻ Navegación intuitiva**
  Interfaz organizada para facilitar el acceso a las diferentes funcionalidades.

* **✦ Experiencia de usuario**
  Diseño moderno y minimalista enfocado en una interacción clara y eficiente.

</details>

---

## Documentación técnica

El repositorio es un **monorepo** con dos aplicaciones independientes que se
comunican por HTTP:

| Carpeta | Tecnología | Descripción |
|---|---|---|
| `pardrops/` | Spring Boot 4 · Java 25 · H2 · JWT | API REST (catálogo, cuentas y pedidos). |
| `pardropsfrontend/` | Angular 22 (standalone + signals) | Interfaz de usuario (SPA). |

### Configuración comercial (Colombia)

La plataforma opera íntegramente en **pesos colombianos (COP)**:

* **Moneda**: todos los importes se guardan y muestran en COP con formato
  `es-CO` (ej. `$ 119.900`, sin decimales). Los productos nacen con
  `currency = "COP"` y, si un producto no trajera moneda, la interfaz asume
  COP en lugar de mostrar otra divisa.
* **IVA**: 19 % sobre el subtotal (`OrderService.TAX_RATE` en el backend y
  `cart.service.ts` en el frontend).
* **Envío**: 25.900 COP, **gratis** a partir de 400.000 COP de subtotal. Los
  mismos umbrales están definidos en el backend (`OrderService`) y replicados
  en el carrito del frontend.
* **Catálogo de ejemplo**: `DataSeeder` siembra los productos con precios COP
  (ej. camiseta 119.900, zapatillas 319.900).
* **Base de datos local**: `pardrops/data/` es prescindible (no está versionada).
  Si contiene datos antiguos en otra moneda, bórrala y el siguiente arranque la
  regenera íntegramente en COP.


### Requisitos

* **JDK 25** para el backend (opcionalmente un JDK portátil en `.tools/jdk25`,
  no versionado; si no existe se usa `JAVA_HOME` o el `java` del `PATH`).
* **Node.js 20+** y `npm` para el frontend.
* No hace falta instalar base de datos: el backend usa **H2 en modo archivo**
  (`pardrops/data/pardropsdb.mv.db`, excluido del control de versiones).

### Cómo ejecutar

**Arranque rápido** (recomendado): en la raíz, en PowerShell

```powershell
.\dev.ps1
```

El script resuelve el JDK (`.tools\jdk25` → `JAVA_HOME` → `java` del `PATH`),
instala las dependencias del frontend la primera vez y abre backend
(`http://localhost:8080`) y frontend (`http://localhost:4200`) en ventanas
separadas. Así dejas el proyecto en marcha en cualquier PC recién clonado.

Para ejecutar el proyecto a mano, sigue los pasos siguientes.

**Backend** (queda escuchando en `http://localhost:8080`):

```powershell
# Backend: usa el JDK portátil incluido (o tu JAVA_HOME / java del PATH)
$env:JAVA_HOME = ".\.tools\jdk25"
cd pardrops
./mvnw.cmd spring-boot:run
```

**Frontend** (queda en `http://localhost:4200`):

```bash
cd pardropsfrontend
npm install
npm start
```

La URL base de la API se configura en
`pardropsfrontend/src/app/core/config/api.config.ts` (`http://localhost:8080/api`).

### Cuentas de prueba

La primera vez que arranca el backend, `DataSeeder` crea automáticamente un
usuario administrador y un catálogo de ejemplo:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@pardrops.com` | `admin123` |

Los clientes pueden **registrarse** libremente desde la pantalla de login.
También se puede comprar **como invitado** (sin crear cuenta).

### Flujos principales

* **Invitado:** navega el catálogo, añade al carrito y compra indicando sus
  datos. El pedido se registra pero **no se guarda en ningún historial**.
* **Cliente registrado**: inicia sesión, compra y puede consultar su historial
  en **Mi cuenta** (`/cuenta`). Cuando un pedido suyo pasa a **entregado**, puede
  valorar en la ficha del producto los artículos que recibió.
* **Administrador**: además, accede al panel de administración para crear,
  editar y eliminar productos con imagen por URL (`/admin/productos`) y para
  gestionar los pedidos (`/admin/pedidos`), donde ve los datos del cliente, la
  dirección de envío y puede cambiar el estado de cada pedido.

### Resumen de la API

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/register` | Público | Crea una cuenta y devuelve el JWT. |
| `POST` | `/api/auth/login` | Público | Inicia sesión y devuelve el JWT. |
| `GET` | `/api/auth/me` | Autenticado | Datos del usuario actual. |
| `GET` | `/api/products` | Público | Lista el catálogo. |
| `GET` | `/api/products/{slug}` | Público | Detalle de un producto. |
| `POST` `PUT` `DELETE` `/api/products/...` | Solo `ADMIN` | Crear, editar y eliminar productos. |
| `GET` | `/api/products/{id}/reviews` | Público | Lista las reseñas de un producto. |
| `GET` | `/api/products/{id}/reviews/eligibility` | Público | Indica si el usuario actual puede reseñar el producto. |
| `POST` | `/api/products/{id}/reviews` | Autenticado | Publica una reseña (solo con compra entregada). |
| `POST` `/api/orders` | Público (invitado o cliente) | Registra un pedido. |
| `GET` `/api/orders/me` | Autenticado | Historial del usuario. |
| `GET` `/api/orders` | Solo `ADMIN` | Todos los pedidos. |
| `PATCH` `/api/orders/{id}/status` | Solo `ADMIN` | Cambia el estado de un pedido. |

Estados de un pedido: `PENDING` (pendiente) → `PAID` (pagado) →
`PREPARED` (preparado) → `SHIPPED` (enviado) → `DELIVERED` (entregado), más
`CANCELLED` (cancelado). Solo se permite avanzar por este flujo (o cancelar
antes de la entrega); `DELIVERED` y `CANCELLED` son finales.

Para publicar una reseña, el cliente debe tener al menos un pedido **entregado**
(`DELIVERED`) que contenga el producto y no haberlo reseñado antes. La
validación se repite en el backend y el autor se toma siempre de la sesión, de
modo que nadie puede publicar reseñas en nombre de otra persona.

> La sesión se transporta en la cabecera `Authorization: Bearer <token>` y el
> backend responde **401** en JSON cuando falta o es inválida.

---

<div align="center">

<h3 style="font-family: Georgia, 'Times New Roman', serif; font-size: 28px;">
  ParDrops
</h3>

<p><i>Explora · Descubre · Compra</i></p>

</div>
