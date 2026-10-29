# Seguridad — Autenticación JWT y roles

**Módulo:** identidad y autorización del backend
**Estado:** implementado (Spring Security 7.1.1 + Nimbus, HS256)
**Última actualización:** octubre de 2026

---

## 1. Qué problema resuelve

Antes, al crear un reporte se enviaba `usuarioId` en el body: cualquier cliente podía
suplantar a otro y subir archivos sin identificarse. Ahora:

- El usuario se **registra o inicia sesión** y recibe un **JWT**.
- Cada petición protegida envía `Authorization: Bearer <token>`.
- El backend obtiene **quién es** y **qué rol tiene** desde el token, no desde el body.
- Los endpoints sensibles se restringen por rol.

---

## 2. Flujo de autenticación

```text
1. POST /api/auth/registro  → crea usuario CIUDADANO + devuelve token
2. POST /api/auth/login     → valida email/contraseña + devuelve token
3. Cliente → Authorization: Bearer <token> en cada petición
4. Spring Security valida firma y expiración, y carga ROLE_<rol>
5. ServicioUsuarioActual obtiene el Usuario desde el SecurityContext
```

---

## 3. Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/registro` | Público | Nombre, email y contraseña (mín. 8). Rol fijo: `CIUDADANO` |
| `POST` | `/api/auth/login` | Público | Email + contraseña → token |
| `GET` | `/api/auth/yo` | Autenticado | Datos del usuario del token |
| `POST` | `/api/reportes` | Autenticado | Crea reporte con el usuario del token |
| `GET` | `/api/reportes` | `FUNCIONARIO` / `ADMINISTRADOR` / `EMPRESA` | Listado paginado; los no administradores solo ven su entidad |
| `GET` | `/api/reportes/mios` | Autenticado | Reportes del usuario del token |
| `GET` | `/api/reportes/{id}` | Dueño / entidad asignada / admin | Detalle con control de alcance |
| `GET` | `/api/reportes/geojson` | `FUNCIONARIO` / `ADMINISTRADOR` / `EMPRESA` | Mapa de reportes |
| `GET` | `/api/dashboard/resumen` | `FUNCIONARIO` / `ADMINISTRADOR` / `EMPRESA` | Métricas del dashboard |
| `GET` | `/api/barrios/geojson`, `/api/sitios-interes/geojson`, `/api/entidades` | `FUNCIONARIO` / `ADMINISTRADOR` / `EMPRESA` | Catálogos del mapa |
| `PATCH` | `/api/reportes/{id}/estado` | `FUNCIONARIO` / `ADMINISTRADOR` | Cambia el estado, respetando la entidad |
| `PATCH` | `/api/reportes/{id}/asignacion` | `ADMINISTRADOR` | Reasigna la entidad responsable |
| Resto de `/api/**` | — | Autenticado | Archivos, etc. |

Respuesta de token:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEnSegundos": 7200,
  "usuario": {
    "id": 124,
    "nombre": "Demo Seguridad",
    "email": "demo@test.local",
    "rol": "CIUDADANO",
    "entidadId": null
  }
}
```

---

## 4. El token JWT

- **Algoritmo:** HS256 (secreto simétrico compartido).
- **Claims:** `sub` (id del usuario), `email`, `nombre`, `rol`, `entidadId` (si aplica).
- **Expiración:** 120 minutos (configurable).
- **Stateless:** no hay sesión ni token en servidor.
- **Roles:** se convierten a autoridades `ROLE_CIUDADANO`, `ROLE_FUNCIONARIO`,
  `ROLE_ADMINISTRADOR`, `ROLE_EMPRESA`.

---

## 5. Reglas de acceso y errores

- Público: solo `POST /api/auth/registro`, `POST /api/auth/login` y la documentación
  (`/v3/api-docs`, `/swagger-ui/**`).
- Todo lo demás (incluido `GET /api/auth/yo`) requiere token válido.
- `GET /api/reportes`, el dashboard y los catálogos exigen `FUNCIONARIO`,
  `ADMINISTRADOR` o `EMPRESA` (`@PreAuthorize`).
- Un `FUNCIONARIO` o `EMPRESA` sin `entidad_id` recibe `403`.
- `PATCH /api/reportes/{id}/estado` exige `FUNCIONARIO` o `ADMINISTRADOR` y que el
  reporte esté asignado a su entidad; el administrador puede cambiar cualquiera.
- `PATCH /api/reportes/{id}/asignacion` exige `ADMINISTRADOR`.
- **401** (sin token, token inválido o expirado):
  `{"error":"No autenticado","mensaje":"Debe enviar un token JWT valido en el header Authorization"}`
- **403** (rol insuficiente o reporte fuera del alcance):
  `{"error":"Acceso denegado","mensaje":"..."}`. El mensaje indica si falta el rol,
  la entidad o el acceso al reporte concreto.

---

## 6. Configuración

En `application.properties`:

```properties
gestion.seguridad.jwt.secreto=${JWT_SECRET:dev-secret-de-al-menos-32-caracteres-cambiar}
gestion.seguridad.jwt.expiracion-minutos=120
```

- **Desarrollo:** usa el valor por defecto (más de 32 caracteres).
- **Producción:** define `JWT_SECRET` como variable de entorno con un valor largo y aleatorio.
  Nunca lo subas al repositorio.

---

## 7. Uso con curl

### Registro

```bash
curl -X POST http://localhost:8080/api/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Demo","email":"demo@test.local","password":"Password123"}'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.local","password":"Password123"}'
```

### Petición autenticada

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -X POST http://localhost:8080/api/reportes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"categoriaIa":"FUGA_AGUA","severidadIa":"MEDIA","latitud":3.4516,"longitud":-76.5320,"claveImagenHd":"incidentes/2026/.../hd.png"}'
```

### Cambio de estado (funcionario/admin)

```bash
curl -X PATCH http://localhost:8080/api/reportes/190/estado \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"estado":"EN_REVISION"}'
```

---

## 8. Cómo probar el rol FUNCIONARIO

El registro público siempre crea `CIUDADANO`. Para probar roles:

```sql
UPDATE usuarios
SET rol = 'FUNCIONARIO', entidad_id = 1
WHERE email = 'demo@test.local';
```

La entidad es obligatoria para que el funcionario pueda ver reportes y cambiar estados.
Luego **vuelve a iniciar sesión** (el token anterior conserva el rol viejo hasta expirar).

Para probar el administrador:

```sql
UPDATE usuarios SET rol = 'ADMINISTRADOR', entidad_id = NULL
WHERE email = 'demo@test.local';
```

---

## 9. Contraseñas

- Se almacenan con **BCrypt** (`PasswordEncoder`).
- La contraseña debe tener entre **8 y 72 caracteres** (límite de BCrypt).
- Los usuarios antiguos con hashes de prueba (`hash-demo`) **no pueden iniciar sesión**;
  deben registrarse de nuevo.

---

## 10. Cambio de contrato (importante)

- Se eliminó `usuarioId` de `ReporteCreacionRequest`.
- Todas las peticiones a `/api/**` (excepto `/api/auth/**`) requieren el header
  `Authorization`.
- El usuario del reporte sale **del token**.

---

## 11. Verificación

- **41/41 tests** en verde.
- `SeguridadTest` cubre: registro/login, contraseña incorrecta, 401 sin token, 401 con token
  inválido, `GET /api/auth/yo` (401 sin token / 200 con token), ciudadano bloqueado en
  `PATCH` (403), funcionario con entidad permitido (200) y que el reporte use el usuario
  del token.
- `DashboardApiTest` cubre el alcance por entidad (funcionario y empresa), detalle y
  cambio de estado entre entidades, filtros combinados, métricas, GeoJSON con fechas
  ISO, catálogos, asignación administrativa, CORS y OpenAPI.
- Prueba manual verificada: registro → 401 sin token → subida autenticada → reporte con
  usuario del token → 403 como ciudadano → 200 tras promover a funcionario de la entidad.

---

## 12. Limitaciones y futuro

- Sin **refresh tokens** ni revocación: el logout consiste en descartar el token.
- Sin **rate limiting** en login (fuerza bruta).
- **HTTPS obligatorio en producción** (el token viaja en el header).
- La creación de funcionarios/administradores se hace por SQL o por un futuro panel admin.
- CORS está configurado para los orígenes de desarrollo del dashboard y Expo; en
  producción hay que ajustar `gestion.cors.origenes-permitidos`.
- OpenAPI/Swagger están habilitados para el equipo; en producción se pueden desactivar con
  `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false`.
- Si se requiere mayor seguridad, migrar a **RS256** y rotación de claves.
