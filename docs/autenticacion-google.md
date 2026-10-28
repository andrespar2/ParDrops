# Autenticación con Google

Documento de diseño del acceso con Google en ParDrops. Forma parte del issue
[#35](https://github.com/andrespar2/ParDrops/issues/35) y sirve de referencia
para el resto del cronograma.

## 1. Alcance

Permitir que un visitante cree una cuenta o inicie sesión con su cuenta de
Google, sin eliminar ni alterar el registro y el inicio de sesión con correo y
contraseña que ya existen.

Al terminar, la plataforma debe ofrecer tres formas de entrar:

1. Correo y contraseña (comportamiento actual).
2. Google (nuevo).
3. Compra como invitado (comportamiento actual, sin historial).

## 2. Inventario de la autenticación actual

| Pieza | Archivo | Responsabilidad |
|---|---|---|
| Entidad de cuenta | `pardrops/.../model/User.java` | Correo único, contraseña, nombre, teléfono, rol y estado |
| Repositorio | `pardrops/.../repository/UserRepository.java` | Búsqueda por correo y listados administrativos |
| Servicio | `pardrops/.../service/AuthService.java` | Registro, login y usuario de la sesión |
| Controlador | `pardrops/.../controller/AuthController.java` | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| JWT | `pardrops/.../security/JwtService.java` | Firma HS256, `sub`, `uid`, `role` y caducidad de 24 h |
| Filtro | `pardrops/.../security/JwtAuthenticationFilter.java` | Valida el token y carga la cuenta desde la base de datos |
| Configuración | `pardrops/.../config/SecurityConfig.java` | Rutas públicas, roles, CORS, 401/403 en JSON |
| Cliente | `pardropsfrontend/src/app/core/services/auth.service.ts` | Signals de sesión y persistencia en `localStorage` |
| Pantalla | `pardropsfrontend/src/app/pages/login/` | Formularios de acceso y registro |
| Guardas | `pardropsfrontend/src/app/core/guards/` | Protección de cuenta, administración y vendedor |

Contrato actual:

```json
{
  "token": "<jwt>",
  "user": {
    "id": 1,
    "email": "cliente@correo.com",
    "fullName": "Cliente",
    "phone": null,
    "role": "CUSTOMER",
    "status": "ACTIVE"
  }
}
```

El token viaja en `Authorization: Bearer <token>`. La API es *stateless* y el
frontend no consulta `/api/auth/me` al arrancar: hidrata la sesión desde
`localStorage`.

## 3. Decisiones de diseño

### 3.1 Flujo OAuth2 con redirección

Se usa el flujo de autorización estándar de Spring Security, no Google
Identity Services en el navegador. Motivos:

- El *client secret* nunca llega al navegador.
- La validación del `state` la hace Spring Security.
- El backend sigue siendo la única autoridad que emite JWT.
- Funciona igual en local y en producción.

```text
/login (Angular)
  └─ botón "Continuar con Google"
       └─ GET http://localhost:8080/oauth2/authorization/google
            └─ Google autentica y vuelve a /login/oauth2/code/google
                 └─ OAuth2AuthenticationSuccessHandler
                      ├─ busca o crea la cuenta
                      ├─ genera un código temporal de un solo uso
                      └─ redirige a http://localhost:4200/auth/callback?code=...
                           └─ POST /api/auth/google/exchange { code }
                                └─ AuthResponse { token, user }
                                     └─ AuthService guarda la sesión y navega a /cuenta
```

El JWT no viaja en la URL. Solo viaja un código opaco, de un solo uso y con
caducidad corta, que el frontend canjea por HTTP.

### 3.2 Sesión sin estado con cookie temporal

`SecurityConfig` usa `SessionCreationPolicy.STATELESS`, pero el flujo de
autorización necesita recordar el `state` entre la ida y la vuelta a Google.
Se añade un `OAuth2AuthorizationRequestRepository` que guarda la solicitud en
una cookie `HttpOnly`, `SameSite=Lax`, limitada a `/` y con caducidad de cinco
minutos. La cookie se elimina al terminar el flujo. No se crea una sesión de
aplicación.

### 3.3 Cuentas sin contraseña local

`users.password` es `NOT NULL`. Para no ejecutar una migración destructiva, las
cuentas de Google reciben una contraseña técnica aleatoria cifrada con BCrypt
que nadie conoce. El login local con ese correo sigue siendo posible solo si el
usuario define una contraseña en el futuro; mientras tanto, la cuenta se
identifica por `provider = GOOGLE` y `providerId = <sub de Google>`.

Esta decisión evita relajar la restricción de la columna y mantiene compatibles
los datos existentes. La migración solo rellena los valores nuevos:

| Columna | Tipo | Valor inicial |
|---|---|---|
| `auth_provider` | `VARCHAR(20) NOT NULL` | `LOCAL` |
| `provider_id` | `VARCHAR(255)` | `NULL` |
| `avatar_url` | `VARCHAR(500)` | `NULL` |

Restricción única para `(auth_provider, provider_id)` cuando `provider_id` no
sea nulo.

### 3.4 Vinculación de cuentas

Si ya existe una cuenta local con el mismo correo:

- Se vincula automáticamente **solo** cuando Google informa
  `email_verified = true`.
- Si la cuenta está suspendida, se rechaza el acceso con un mensaje claro.
- Si Google no confirma el correo, se rechaza la vinculación y se pide usar el
  acceso local.

Nunca se crea una segunda cuenta con el mismo correo: el correo es la clave
natural de la cuenta en ParDrops.

### 3.5 Código de intercambio

- Formato: 32 bytes aleatorios en Base64 URL-safe.
- Vida: 2 minutos.
- Un solo uso: se elimina al canjearlo, aunque la respuesta falle.
- Almacenamiento en memoria (`ConcurrentHashMap`) con limpieza periódica. Si en
  el futuro hay varias instancias, se sustituirá por una tabla o Redis.
- En los logs solo se registra el identificador de la cuenta, nunca el código.

## 4. Modelo de datos

### `User`

| Campo | Cambio |
|---|---|
| `authProvider` | Nuevo, `LOCAL` por defecto |
| `providerId` | Nuevo, `sub` de Google, opcional |
| `avatarUrl` | Nuevo, foto de perfil, opcional |
| `password` | Sin cambios: sigue siendo obligatoria |

### `UserResponse`

Se añaden `provider` y `avatarUrl`. Los consumidores actuales siguen recibiendo
los mismos campos, así que el cambio es compatible.

## 5. API

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/oauth2/authorization/google` | Público | Inicia el flujo con Google |
| `GET` | `/login/oauth2/code/google` | Público | Callback que atiende Spring Security |
| `POST` | `/api/auth/google/exchange` | Público | Canjea el código temporal por `AuthResponse` |
| `GET` | `/api/auth/me` | Sesión | Sin cambios |

Errores:

| Situación | Respuesta |
|---|---|
| Google no configurado | Redirección a `/login?oauth=no-configurado` |
| Usuario cancela en Google | Redirección a `/login?oauth=cancelado` |
| Correo no verificado | Redirección a `/login?oauth=correo-no-verificado` |
| Cuenta suspendida | Redirección a `/login?oauth=cuenta-suspendida` |
| Código inválido o caducado | `401` con mensaje legible en el canje |

## 6. Seguridad

- El `client secret` solo vive en variables de entorno del backend.
- El JWT se entrega en el cuerpo de una petición HTTP autenticada por el código
  temporal, nunca en la URL.
- El código temporal es de un solo uso y caduca en dos minutos.
- Solo se vinculan cuentas con correo verificado por Google.
- Las cuentas suspendidas se rechazan antes de emitir el JWT.
- El `state` y el `PKCE` los gestiona Spring Security.
- En producción el backend y el frontend deben servirse por HTTPS; las cookies
  llevarán `Secure` cuando `app.oauth2.secure-cookies=true`.
- Los códigos y los tokens no se escriben en los logs.

## 7. Configuración

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET:}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}

app.oauth2.frontend-url=${APP_FRONTEND_URL:http://localhost:4200}
app.oauth2.success-path=/auth/callback
app.oauth2.failure-path=/login
app.oauth2.code-ttl-seconds=120
app.oauth2.secure-cookies=${APP_OAUTH2_SECURE_COOKIES:false}
```

En Google Cloud Console:

1. Crear un proyecto y configurar la pantalla de consentimiento.
2. Crear credenciales de tipo "Aplicación web".
3. Añadir el origen autorizado `http://localhost:4200`.
4. Añadir el URI de redirección
   `http://localhost:8080/login/oauth2/code/google`.

La guía detallada se completa en el issue #42.

## 8. Plan de implementación

| Issue | Etapa | Entrega |
|---|---|---|
| #36 | Backend y base de datos | Modelo, repositorio, DTO y migración |
| #37 | Backend y base de datos | OAuth2 en Spring Security y cookies de estado |
| #38 | Backend y base de datos | Cuenta, vinculación, JWT y canje del código |
| #39 | Interfaz de acceso | Botón de Google en el login |
| #40 | Interfaz de acceso | Callback, sesión y avatar |
| #41 | Pruebas y documentación | Suites de backend y frontend |
| #42 | Pruebas y documentación | Guía de Google Cloud y README |

## 9. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| El contexto de Spring falla sin credenciales | `oauth2Login` condicionado y registro simulado en pruebas |
| El callback devuelve 401 | Rutas de OAuth públicas y *entry point* específico |
| Se duplican cuentas por correo | Búsqueda por correo antes de crear y restricción única |
| Se vincula una cuenta ajena | Solo con `email_verified = true` |
| El JWT queda en el historial | Código temporal de un solo uso |
| El `state` se pierde | Cookie `HttpOnly` específica del flujo |
| El avatar externo falla | Respaldo con la inicial del nombre |

## 10. Estrategia de ramas y commits

- Rama de trabajo: `feature/google-auth`, creada desde `main`.
- Un commit por unidad lógica, en español y con el formato
  `tipo(alcance): descripción` (`feat`, `fix`, `test`, `docs`, `refactor`).
- Cada issue se cierra en el mismo commit que lo completa, con `Closes #N` en el
  cuerpo del commit o del pull request.
- Al terminar, se abre un pull request hacia `main` y se enlaza con el Project.
- Las pruebas de backend (`.\mvnw.cmd -o test`), frontend (`npm test`) y el
  build (`npm run build`) deben pasar antes de cerrar cada issue de código.
