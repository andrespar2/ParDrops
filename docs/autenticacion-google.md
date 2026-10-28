# Autenticación con Google

Guía técnica y de configuración del acceso con Google en ParDrops. Forma parte
del proyecto [ParDrops - Inicio de sesión con Google](https://github.com/users/andrespar2/projects/3)
y documenta el diseño, la implementación y los pasos para ponerlo en marcha.

## 1. Qué permite

Un visitante puede crear una cuenta o iniciar sesión con su cuenta de Google.
La plataforma mantiene las tres formas de entrar:

1. Correo y contraseña (comportamiento original).
2. Google (nuevo).
3. Compra como invitado (sin historial).

El JWT y el contrato de `/api/auth/login`, `/api/auth/register` y
`/api/auth/me` no cambian: Google solo añade otra vía para obtener el mismo
`AuthResponse`.

## 2. Requisitos

- Java 25 y el JDK del proyecto (`.tools/jdk25` o `JAVA_HOME`).
- Node.js 20 o superior.
- Una cuenta de Google Cloud con posibilidad de crear credenciales OAuth.
- Backend en `http://localhost:8080` y frontend en `http://localhost:4200`.

## 3. Configurar Google Cloud

### 3.1 Crear el proyecto

1. Entra en [Google Cloud Console](https://console.cloud.google.com/).
2. Crea un proyecto (por ejemplo, `ParDrops`).
3. Selecciona el proyecto en la barra superior.

### 3.2 Configurar la pantalla de consentimiento

1. Ve a **APIs y servicios → Pantalla de consentimiento de OAuth**.
2. Elige **Externo** si la aplicación estará disponible para cualquier cuenta
   de Google, o **Interno** si solo será para usuarios de una organización.
3. Completa el nombre de la aplicación (`ParDrops`), el correo de asistencia y
   el logo si lo tienes.
4. Añade los ámbitos `openid`, `profile` y `email`.
5. Añade usuarios de prueba mientras la aplicación esté en modo de prueba.

### 3.3 Crear las credenciales

1. Ve a **APIs y servicios → Credenciales**.
2. Pulsa **Crear credenciales → ID de cliente de OAuth**.
3. Tipo de aplicación: **Aplicación web**.
4. Orígenes autorizados de JavaScript:
   - `http://localhost:4200`
   - `http://localhost:4000` (SSR, si se usa)
5. URIs de redirección autorizados:
   - `http://localhost:8080/login/oauth2/code/google`
6. Guarda el **Client ID** y el **Client Secret**.

> El secreto nunca debe subirse al repositorio. Se inyecta por variable de
> entorno.

### 3.4 Configurar el backend

PowerShell:

```powershell
$env:GOOGLE_CLIENT_ID = 'tu-client-id.apps.googleusercontent.com'
$env:GOOGLE_CLIENT_SECRET = 'tu-client-secret'
.\dev.ps1
```

Bash:

```bash
export GOOGLE_CLIENT_ID="tu-client-id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="tu-client-secret"
```

Si el frontend o el backend se sirven en otra URL, ajusta:

| Variable | Valor por defecto | Uso |
|---|---|---|
| `APP_FRONTEND_URL` | `http://localhost:4200` | Base de las redirecciones al frontend |
| `APP_OAUTH2_SECURE_COOKIES` | `false` | Marca la cookie del flujo como `Secure` (HTTPS) |

No hace falta tocar `application.properties`: los valores por defecto ya están
preparados para desarrollo.

## 4. Flujo completo

```text
/login (Angular)
  └─ "Continuar con Google"
       └─ GET http://localhost:8080/oauth2/authorization/google
            └─ Google autentica y vuelve a /login/oauth2/code/google
                 └─ OAuth2AuthenticationSuccessHandler
                      ├─ busca, crea o vincula la cuenta
                      ├─ emite un código temporal de un solo uso
                      └─ redirige a http://localhost:4200/auth/callback?code=...
                           └─ POST /api/auth/google/exchange { code }
                                └─ AuthResponse { token, user }
                                     └─ AuthService guarda la sesión y navega
                                        a /cuenta o a la ruta solicitada
```

Decisiones clave:

- El JWT **no** viaja en la URL. Viaja un código opaco que el frontend canjea
  por HTTP.
- El `state` y el `PKCE` se guardan en una cookie `HttpOnly`, `SameSite=Lax`,
  con cinco minutos de vida, porque la API es *stateless*.
- El código de canje dura 120 segundos, se invalida al emitir otro para la
  misma cuenta y solo se puede usar una vez.
- El canje está limitado a 10 intentos por minuto y por origen (`429` al
  superarlo).
- Si Google cancela, el backend redirige a `/login?oauth=cancelado`.

## 5. Cuentas y vinculación

| Caso | Comportamiento |
|---|---|
| No existe la cuenta | Se crea con rol `CUSTOMER`, estado `ACTIVE` y proveedor `GOOGLE` |
| Existe una cuenta local con el mismo correo verificado | Se vincula a Google sin duplicarla y se avisa al usuario |
| Existe una cuenta local pero Google no confirma el correo | Se rechaza la vinculación |
| La cuenta está suspendida | Se rechaza el acceso antes de emitir el JWT |
| La cuenta ya está vinculada | Se reutiliza y se actualizan el nombre y el avatar |

Las cuentas de Google reciben una contraseña técnica aleatoria cifrada con
BCrypt que nadie conoce. Así se mantiene la restricción `password NOT NULL`
sin migraciones destructivas y el login local sigue funcionando para las
cuentas que sí tienen contraseña.

Al vincular una cuenta local se conserva el nombre que el usuario había
elegido, se actualiza la foto y se envía un aviso de seguridad a la campanilla.

## 6. Modelo de datos

### `User`

| Campo | Tipo | Descripción |
|---|---|---|
| `authProvider` | `LOCAL` / `GOOGLE` | Origen de la autenticación |
| `providerId` | texto | `sub` de Google; único junto con el proveedor |
| `avatarUrl` | texto | Foto de perfil |

La migración `GoogleSchemaMigration` marca las cuentas existentes como `LOCAL`
y crea el índice único `uk_users_provider`. Es idempotente.

### `UserResponse`

Se añaden `provider` y `avatarUrl`. El resto del contrato no cambia.

## 7. API

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/oauth2/authorization/google` | Público | Inicia el flujo con Google |
| `GET` | `/login/oauth2/code/google` | Público | Callback que atiende Spring Security |
| `POST` | `/api/auth/google/exchange` | Público (limitado) | Canjea el código por `AuthResponse` |
| `GET` | `/api/auth/me` | Sesión | Sin cambios |

Ejemplo de canje:

```http
POST /api/auth/google/exchange
Content-Type: application/json

{ "code": "codigo-temporal" }
```

Respuesta correcta:

```json
{
  "token": "<jwt>",
  "user": {
    "id": 12,
    "email": "cliente@gmail.com",
    "fullName": "Cliente",
    "phone": null,
    "role": "CUSTOMER",
    "status": "ACTIVE",
    "provider": "GOOGLE",
    "avatarUrl": "https://lh3.googleusercontent.com/a/..."
  }
}
```

Errores:

| Situación | Respuesta |
|---|---|
| Google no configurado | La ruta de inicio no existe (404) y el resto de accesos sigue funcionando |
| Usuario cancela en Google | Redirección a `/login?oauth=cancelado` |
| Correo no verificado | Redirección a `/login?oauth=correo-no-verificado` |
| Cuenta suspendida | Redirección a `/login?oauth=cuenta-suspendida` |
| Código inválido o caducado | `401` con mensaje legible |
| Demasiados intentos | `429` con mensaje legible |

## 8. Seguridad

- El *client secret* solo existe en el entorno del backend.
- El JWT se entrega en el cuerpo de una petición HTTP autenticada por el código
  temporal, nunca en la URL.
- Solo se vinculan cuentas con el correo verificado por Google.
- Las cuentas suspendidas se rechazan antes de emitir el JWT.
- El código temporal caduca, se invalida al emitir otro y no se registra en los
  logs.
- El endpoint de canje tiene un límite de intentos por origen.
- En producción hay que usar HTTPS y activar
  `APP_OAUTH2_SECURE_COOKIES=true`.
- La cookie del flujo es `HttpOnly` y se elimina al terminar.

## 9. Pruebas

```powershell
# Backend
cd pardrops
$env:JAVA_HOME = (Resolve-Path '..\.tools\jdk25').Path
.\mvnw.cmd -o test

# Frontend
cd ..\pardropsfrontend
npx ng test --watch=false
npm run build
```

Cobertura añadida:

| Suite | Pruebas |
|---|---|
| Backend | 179 (modelo, migración, servicio, código, límite, canje y flujo completo) |
| Frontend | 75 en 16 archivos (servicio, login, callback y avatar) |

Las pruebas no necesitan credenciales reales de Google: el registro se simula
con propiedades de prueba y el flujo se valida sin salir a internet.

## 10. Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| `redirect_uri_mismatch` | La URI no coincide exactamente | Revisa `http://localhost:8080/login/oauth2/code/google` en Google Cloud |
| El botón devuelve 404 | Faltan credenciales | Define `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` y reinicia el backend |
| `invalid_client` | Client ID o secret incorrectos | Copia de nuevo las credenciales |
| `Error 400: invalid_request` | Origen no autorizado | Añade `http://localhost:4200` a los orígenes de JavaScript |
| El callback vuelve a `/login?oauth=error` | Fallo al crear o vincular la cuenta | Revisa el log del backend y la cuenta en la base de datos |
| La foto no aparece | Google no devolvió `picture` o la URL expiró | La interfaz usa la inicial como respaldo |
| `429` al canjear | Se superaron 10 intentos en un minuto | Espera un minuto y reintenta |

## 11. Referencias

- [Spring Security: OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html)
- [Google Identity: OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect)
- [GitHub Project del cambio](https://github.com/users/andrespar2/projects/3)
