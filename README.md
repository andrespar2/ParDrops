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

### Requisitos

* **JDK 25** para el backend (se incluye un JDK portátil en `.tools/jdk25`, no
  versionado).
* **Node.js 20+** y `npm` para el frontend.
* No hace falta instalar base de datos: el backend usa **H2 en modo archivo**
  (`pardrops/data/pardropsdb.mv.db`, excluido del control de versiones).

### Cómo ejecutar

**Backend** (queda escuchando en `http://localhost:8080`):

```powershell
# Windows: usar el JDK portátil incluido
$env:JAVA_HOME = "..\.tools\jdk25"
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
  en **Mi cuenta** (`/cuenta`).
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
| `POST` `/api/orders` | Público (invitado o cliente) | Registra un pedido. |
| `GET` `/api/orders/me` | Autenticado | Historial del usuario. |
| `GET` `/api/orders` | Solo `ADMIN` | Todos los pedidos. |
| `PATCH` `/api/orders/{id}/status` | Solo `ADMIN` | Cambia el estado de un pedido. |

Estados de un pedido: `PENDING` (pendiente) → `PAID` (pagado) →
`PREPARED` (preparado) → `SHIPPED` (enviado) → `DELIVERED` (entregado), más
`CANCELLED` (cancelado). Solo se permite avanzar por este flujo (o cancelar
antes de la entrega); `DELIVERED` y `CANCELLED` son finales.

> La sesión se transporta en la cabecera `Authorization: Bearer <token>` y el
> backend responde **401** en JSON cuando falta o es inválida.

---

<div align="center">

<h3 style="font-family: Georgia, 'Times New Roman', serif; font-size: 28px;">
  ParDrops
</h3>

<p><i>Explora · Descubre · Compra</i></p>

</div>
