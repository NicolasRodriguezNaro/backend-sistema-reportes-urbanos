# Guía de integración del frontend

**Proyecto:** Plataforma Inteligente de Gestión de Incidencias Urbanas
**Backend:** `gestion-backend` (Spring Boot 4.1.2)
**Última actualización:** octubre de 2026
**Audiencia:** equipo de frontend (React, React Native/Expo)

Esta guía explica **qué endpoints puede consumir el frontend, qué recibe, qué devuelve y para qué sirve**. Está pensada para que puedas construir las pantallas y los servicios de datos sin tener que leer el código Java.

> La fuente de verdad siempre es el código del backend y Swagger (`/swagger-ui.html`). Si esta guía y Swagger difieren, revisa primero el endpoint en Swagger.

---

## 1. Resumen rápido

| Método | Endpoint | Quién puede usarlo | Para qué |
|---|---|---|---|
| `POST` | `/api/auth/registro` | Público | Registrar un ciudadano y obtener token |
| `POST` | `/api/auth/login` | Público | Iniciar sesión y obtener token |
| `GET` | `/api/auth/yo` | Autenticado | Consultar el usuario del token |
| `POST` | `/api/archivos` | Autenticado | Subir foto HD y generar miniatura |
| `GET` | `/api/archivos/{clave}` | Autenticado | Descargar un archivo (modo local) |
| `POST` | `/api/reportes` | Autenticado | Crear un reporte |
| `GET` | `/api/reportes/{id}` | Según alcance | Ver el detalle de un reporte |
| `GET` | `/api/reportes` | Funcionario/Admin/Empresa | Listado paginado con filtros |
| `GET` | `/api/reportes/mios` | Autenticado | Reportes del usuario actual |
| `GET` | `/api/reportes/geojson` | Funcionario/Admin/Empresa | Puntos para el mapa |
| `PATCH` | `/api/reportes/{id}/estado` | Funcionario/Admin | Cambiar el estado |
| `PATCH` | `/api/reportes/{id}/asignacion` | Administrador | Reasignar la entidad responsable |
| `GET` | `/api/dashboard/resumen` | Funcionario/Admin/Empresa | Métricas y tarjetas del dashboard |
| `GET` | `/api/barrios/geojson` | Funcionario/Admin/Empresa | Capa GeoJSON de barrios |
| `GET` | `/api/sitios-interes/geojson` | Funcionario/Admin/Empresa | Capa GeoJSON de sitios de interés |
| `GET` | `/api/entidades` | Funcionario/Admin/Empresa | Catálogo de entidades |

Roles existentes:

```text
CIUDADANO
FUNCIONARIO
ADMINISTRADOR
EMPRESA
```

Resumen de alcance:

- **CIUDADANO:** app móvil. Crea reportes y consulta los suyos.
- **FUNCIONARIO:** dashboard. Solo ve reportes asignados a su entidad.
- **EMPRESA:** dashboard. Mismo alcance que funcionario.
- **ADMINISTRADOR:** dashboard completo. Ve todo y puede reasignar.

---

## 2. URL base y entornos

### Desarrollo local

```text
http://localhost:8080
```

La documentación interactiva está en:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

### React web con Vite

Crea un archivo `.env` en el frontend:

```env
VITE_API_URL=http://localhost:8080
```

Y úsalo así:

```ts
const API_BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
```

### Expo / React Native

Para un emulador o un dispositivo físico, `localhost` apunta al dispositivo, no a tu computador. Usa:

| Entorno | URL base |
|---|---|
| Web de Expo | `http://localhost:8080` |
| Emulador Android | `http://10.0.2.2:8080` |
| Simulador iOS | `http://localhost:8080` |
| Dispositivo físico | `http://<IP-LAN-de-tu-PC>:8080` |

Ejemplo de variable pública de Expo:

```env
EXPO_PUBLIC_API_URL=http://192.168.1.50:8080
```

```ts
const API_BASE = process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080";
```

> En un dispositivo físico, el teléfono y el computador deben estar en la misma red.
> También debes verificar que el backend sea accesible desde la red, no solo desde `localhost`.

### Orígenes CORS

El backend permite por defecto:

```text
http://localhost:5173     (React/Vite)
http://localhost:8081     (Expo)
http://localhost:19006    (Expo web)
```

Si usas otro puerto, cambia `gestion.cors.origenes-permitidos` en `application.properties` y reinicia el backend. CORS solo afecta a navegadores; las peticiones nativas de React Native no pasan por CORS.

### Probar con Swagger

1. Abre `http://localhost:8080/swagger-ui.html`.
2. Ejecuta `POST /api/auth/login` con un usuario de prueba.
3. Copia el token de la respuesta, **sin** el prefijo `Bearer`.
4. Haz clic en **Authorize** y pega el token.
5. Prueba los endpoints protegidos.

Las rutas de documentación son públicas, pero los endpoints de negocio siguen exigiendo
un token válido.

---

## 3. Convenciones de la API

### Formato

- Todas las peticiones y respuestas usan **JSON**, excepto la subida de archivos.
- La subida de archivos usa `multipart/form-data`.
- Las fechas son ISO 8601, por ejemplo:

```text
2026-10-27T18:00:00Z
```

- Los filtros de fecha usan solo el día:

```text
YYYY-MM-DD
```

### Autenticación

Los endpoints protegidos necesitan este header:

```http
Authorization: Bearer <token>
```

El token se obtiene con `/api/auth/registro` o `/api/auth/login`. No se usan cookies de sesión.

### Enums

```text
EstadoReporte:       PENDIENTE, EN_REVISION, ASIGNADO, RESUELTO, RECHAZADO
TipoIncidencia:      HUECO_VIA, FUGA_AGUA, FALLA_ALUMBRADO, ACUMULACION_BASURA
Severidad:           BAJA, MEDIA, ALTA, CRITICA
PrioridadCalculada:  BAJA, MEDIA, ALTA, CRITICA
RolUsuario:          CIUDADANO, FUNCIONARIO, ADMINISTRADOR, EMPRESA
TipoEntidad:         SECRETARIA_OBRAS_PUBLICAS, EMPRESA_SERVICIOS_PUBLICOS, EMPRESA_ASEO
```

Los valores deben enviarse exactamente en mayúsculas, sin tildes y sin espacios.

### Paginación

Los listados paginados aceptan:

| Parámetro | Default | Descripción |
|---|---:|---|
| `page` | `0` | Página, empieza en 0 |
| `size` | `20` | Elementos por página, máximo 100 |

La respuesta tiene esta forma:

```json
{
  "contenido": [],
  "pagina": 0,
  "tamano": 20,
  "total": 0,
  "totalPaginas": 0
}
```

El backend normaliza valores raros: una página negativa pasa a 0 y un tamaño fuera de rango se ajusta entre 1 y 100.

### Errores

Los errores tienen un cuerpo JSON. Los formatos principales son:

**Validación de campos (`400`):**

```json
{
  "error": "Datos invalidos",
  "campos": {
    "latitud": "La latitud es obligatoria",
    "claveImagenHd": "La clave de la imagen HD es obligatoria"
  }
}
```

**Regla de negocio (`400`):**

```json
{
  "error": "Solicitud invalida",
  "mensaje": "Ya existe un usuario registrado con ese email"
}
```

**No autenticado (`401`):**

```json
{
  "error": "No autenticado",
  "mensaje": "Debe enviar un token JWT valido en el header Authorization"
}
```

**Prohibido (`403`):**

```json
{
  "error": "Acceso denegado",
  "mensaje": "El reporte no pertenece a su entidad"
}
```

**No encontrado (`404`):**

```json
{
  "error": "No encontrado",
  "mensaje": "Reporte no encontrado con id 999"
}
```

**Archivo demasiado grande (`413`):**

```json
{
  "error": "Archivo demasiado grande",
  "mensaje": "La imagen supera el tamano maximo permitido (10 MB)"
}
```

Recomendación para el cliente:

- `401`: borrar token y volver al login.
- `403`: mostrar mensaje de permisos; **no** cerrar sesión necesariamente.
- `400`: mostrar los errores de `campos` junto al formulario.
- `404`: mostrar “no encontrado” o volver al listado.
- `413`: pedir una imagen más pequeña.

---

## 4. Autenticación

### 4.1 Registro de ciudadano

```http
POST /api/auth/registro
Content-Type: application/json
```

Body:

```json
{
  "nombre": "Ana Pérez",
  "email": "ana@example.com",
  "password": "Password123"
}
```

Reglas:

- `nombre`: obligatorio, máximo 150 caracteres.
- `email`: obligatorio, válido, máximo 150 caracteres. El backend lo guarda en minúsculas.
- `password`: obligatorio, entre 8 y 72 caracteres.
- El rol siempre es `CIUDADANO`; no se puede elegir desde el registro.
- Si el email ya existe, la respuesta es `400` con
  `{"error":"Solicitud invalida","mensaje":"Ya existe un usuario registrado con ese email"}`.

Respuesta `201 Created`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEnSegundos": 7200,
  "usuario": {
    "id": 124,
    "nombre": "Ana Pérez",
    "email": "ana@example.com",
    "rol": "CIUDADANO",
    "entidadId": null
  }
}
```

### 4.2 Login

```http
POST /api/auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "ana@example.com",
  "password": "Password123"
}
```

Respuesta `200 OK`: mismo cuerpo que el registro.

Errores:

- `401`: email o contraseña incorrectos.
- `400`: email inválido o campos faltantes.

### 4.3 Usuario actual

```http
GET /api/auth/yo
Authorization: Bearer <token>
```

Respuesta `200 OK`:

```json
{
  "id": 124,
  "nombre": "Ana Pérez",
  "email": "ana@example.com",
  "rol": "CIUDADANO",
  "entidadId": null
}
```

### 4.4 Manejo del token en el frontend

- Guarda el token después del login.
- En web, para este proyecto académico puedes usar `localStorage`; ten en cuenta el riesgo de XSS.
- En React Native, usa `expo-secure-store` o almacenamiento seguro equivalente.
- Envía el token en cada petición protegida.
- No hay refresh token: cuando el backend responda `401`, vuelve al login.
- Si cambias el rol de un usuario directamente en la base de datos, el token anterior conserva el rol viejo hasta que expire. Vuelve a iniciar sesión.

### 4.5 Usuarios de prueba para el dashboard

El registro público **solo crea ciudadanos**. No existe un endpoint para crear
funcionarios o administradores desde el frontend. Para probar el dashboard, el equipo
del backend debe promover un usuario en la base de datos:

```sql
-- Funcionario de la entidad 1
UPDATE usuarios
SET rol = 'FUNCIONARIO', entidad_id = 1
WHERE email = 'funcionario@example.com';

-- Administrador
UPDATE usuarios
SET rol = 'ADMINISTRADOR', entidad_id = NULL
WHERE email = 'admin@example.com';
```

Después del cambio, **cierra sesión y vuelve a entrar** para obtener un token con el rol
nuevo. La entidad `1` debe existir en la tabla `entidades`.

---

## 5. Archivos e imágenes

El flujo es de **dos pasos**:

```text
1. POST /api/archivos  → subir imagen, recibir claves
2. POST /api/reportes  → crear reporte usando las claves
```

Nunca envíes la URL como `claveImagenHd`; el backend espera la **clave** del objeto.

### 5.1 Subir una imagen

```http
POST /api/archivos
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

Campo obligatorio del formulario:

```text
archivo
```

Reglas:

- Formatos: `image/jpeg`, `image/png`, `image/webp`.
- Tamaño máximo: 10 MB.
- El servidor genera la clave; no la inventes.
- La miniatura se genera a 320 px de ancho cuando `ImageIO` puede leer el formato.

Respuesta `201 Created`:

```json
{
  "clave": "incidentes/2026/10/27/1e841516-d57b-4db3-bb2c-856d2d633fbd/hd.jpg",
  "url": "http://localhost:9000/reportes/incidentes/.../hd.jpg?X-Amz-Signature=...",
  "claveThumb": "incidentes/2026/10/27/1e841516-d57b-4db3-bb2c-856d2d633fbd/thumb.jpg",
  "urlThumb": "http://localhost:9000/reportes/incidentes/.../thumb.jpg?X-Amz-Signature=..."
}
```

Notas:

- `clave` y `url` corresponden a la imagen HD.
- `claveThumb` y `urlThumb` pueden ser `null` si no se pudo generar la miniatura
  (por ejemplo, un WEBP que `ImageIO` no puede decodificar).
- Si `url` empieza con `http://localhost:9000/...`, estás usando el adaptador S3/Silo
  (perfil `dev,minio`); esa URL es firmada y temporal.
- Si `url` empieza con `http://localhost:8080/api/archivos/...`, estás usando
  almacenamiento local. **Ese endpoint exige token**, así que no puedes usarlo
  directamente en un `<img src="...">` sin autenticación.

### 5.2 Mostrar imágenes en el frontend

#### Con S3/Silo

La URL firmada se puede usar directamente:

```tsx
<img src={reporte.urlImagenThumb ?? ""} alt="Miniatura" />
```

En React Native:

```tsx
<Image source={{ uri: reporte.urlImagenThumb ?? undefined }} />
```

#### Con almacenamiento local

Como `/api/archivos/**` requiere token, tienes dos opciones:

1. Usar el perfil `dev,minio` para que las URLs sean firmadas y no necesiten header.
2. Descargar la imagen con `fetch`, enviar el token y crear un object URL:

```ts
async function cargarImagenProtegida(url: string, token: string) {
  const respuesta = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!respuesta.ok) throw new Error("No se pudo cargar la imagen");

  const blob = await respuesta.blob();
  return URL.createObjectURL(blob);
}
```

En React Native local puedes usar `expo-file-system` o el mismo patrón con
`fetch` y un archivo temporal.

### 5.3 Descargar un archivo en modo local

```http
GET /api/archivos/{clave}
Authorization: Bearer <token>
```

Ejemplo:

```text
GET /api/archivos/incidentes/2026/10/27/uuid/hd.jpg
```

Devuelve el contenido binario con su `Content-Type`. No es necesario usar este endpoint
en el flujo normal si ya tienes una URL firmada.

### 5.4 Ejemplo de subida con web

```ts
async function subirImagen(file: File, token: string): Promise<ArchivoResponse> {
  const formData = new FormData();
  formData.append("archivo", file);

  const respuesta = await fetch(`${API_BASE}/api/archivos`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      // NO agregues Content-Type manualmente:
      // el navegador debe agregar el boundary del multipart.
    },
    body: formData,
  });

  if (!respuesta.ok) {
    throw new Error("No se pudo subir la imagen");
  }

  return respuesta.json() as Promise<ArchivoResponse>;
}
```

### 5.5 Ejemplo de subida con React Native/Expo

```ts
import * as ImagePicker from "expo-image-picker";

const resultado = await ImagePicker.launchImageLibraryAsync({
  mediaTypes: ImagePicker.MediaTypeOptions.Images,
  quality: 0.8,
});

if (!resultado.canceled) {
  const asset = resultado.assets[0];
  const formData = new FormData();

  formData.append("archivo", {
    uri: asset.uri,
    name: asset.fileName ?? `foto-${Date.now()}.jpg`,
    type: asset.mimeType ?? "image/jpeg",
  } as any);

  const respuesta = await fetch(`${API_BASE}/api/archivos`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });

  const archivo = (await respuesta.json()) as ArchivoResponse;
  console.log(archivo.clave, archivo.claveThumb);
}
```

> El `as any` es necesario porque el tipo `FormData` del DOM no conoce el objeto
> `{ uri, name, type }` que usa React Native.

> El backend puede ejecutarse con el perfil `dev` (almacenamiento local) o con
> `dev,minio` (S3/Silo). Pide al responsable del backend que confirme cuál está activo,
> porque cambia la forma de las URLs de las imágenes.

---

## 6. Reportes

### 6.1 Crear un reporte

```http
POST /api/reportes
Authorization: Bearer <token>
Content-Type: application/json
```

Body mínimo:

```json
{
  "latitud": 3.4516,
  "longitud": -76.532,
  "claveImagenHd": "incidentes/2026/10/27/uuid/hd.jpg"
}
```

Body completo, si el frontend quiere enviar la clasificación:

```json
{
  "descripcion": "Fuga de agua frente al hospital",
  "categoriaIa": "FUGA_AGUA",
  "severidadIa": "ALTA",
  "latitud": 3.4516,
  "longitud": -76.532,
  "claveImagenHd": "incidentes/2026/10/27/uuid/hd.jpg",
  "claveImagenThumb": "incidentes/2026/10/27/uuid/thumb.jpg"
}
```

Reglas:

- `latitud` y `longitud`: obligatorias, en grados WGS84.
- `claveImagenHd`: obligatoria.
- `claveImagenThumb`: opcional.
- `descripcion`: opcional, máximo 2000 caracteres.
- `categoriaIa` y `severidadIa`: opcionales. Si no se envían, el backend las completa
  con el clasificador de IA.
- `usuarioId` **no se envía**; el backend usa el usuario del token.
- El estado inicial es `PENDIENTE`.
- La prioridad y la entidad asignada las calcula el backend.

Respuesta `201 Created`:

```json
{
  "id": 312,
  "estado": "PENDIENTE",
  "categoriaIa": "FUGA_AGUA",
  "severidadIa": "ALTA",
  "prioridadCalculada": "CRITICA",
  "confianzaIa": null,
  "descripcion": "Fuga de agua frente al hospital",
  "latitud": 3.4516,
  "longitud": -76.532,
  "claveImagenHd": "incidentes/2026/10/27/uuid/hd.jpg",
  "urlImagenHd": "http://localhost:9000/reportes/...?X-Amz-Signature=...",
  "claveImagenThumb": "incidentes/2026/10/27/uuid/thumb.jpg",
  "urlImagenThumb": "http://localhost:9000/reportes/...?X-Amz-Signature=...",
  "fechaCreacion": "2026-10-27T18:00:00Z",
  "fechaActualizacion": "2026-10-27T18:00:00Z",
  "usuarioId": 124,
  "barrioId": 124,
  "entidadAsignadaId": 2,
  "reportePadreId": null
}
```

El header `Location` apunta a `/api/reportes/{id}`.

`confianzaIa` vale `null` cuando el frontend envió todos los campos de clasificación.
Si se omitió `categoriaIa`, `severidadIa` o `descripcion`, el backend usa el clasificador
y devuelve un valor numérico de confianza.

#### Deduplicación

Si se crea un reporte a menos de **5 metros** de otro con la **misma categoría**, el
backend lo enlaza con el reporte raíz:

```json
{
  "id": 313,
  "reportePadreId": 312
}
```

El campo `reportePadreId` puede ser `null` si el reporte es independiente. Para el mapa
puedes mostrar todos los puntos o agrupar los que comparten `reportePadreId`.

### 6.2 Consultar el detalle

```http
GET /api/reportes/{id}
Authorization: Bearer <token>
```

Devuelve el objeto completo de la sección anterior.

Alcance:

- **CIUDADANO:** solo sus propios reportes.
- **FUNCIONARIO/EMPRESA:** solo reportes asignados a su entidad.
- **ADMINISTRADOR:** cualquiera.

Si no tiene acceso, devuelve `403`. Si no existe, `404`.

### 6.3 Listado paginado

```http
GET /api/reportes
Authorization: Bearer <token>
```

Roles: `FUNCIONARIO`, `ADMINISTRADOR` o `EMPRESA`.

Filtros opcionales:

| Parámetro | Tipo | Ejemplo | Descripción |
|---|---|---|---|
| `estado` | enum | `PENDIENTE` | Estado del reporte |
| `categoria` | enum | `FUGA_AGUA` | Categoría de incidencia |
| `prioridad` | enum | `CRITICA` | Prioridad calculada |
| `barrioId` | entero | `124` | `gid` del barrio |
| `entidadId` | entero | `2` | Solo el admin puede filtrar por otra entidad |
| `desde` | fecha | `2026-10-01` | Desde el inicio de ese día, inclusive |
| `hasta` | fecha | `2026-10-31` | Hasta el final de ese día, inclusive |
| `texto` | texto | `fuga` | Búsqueda parcial en la descripción |
| `page` | entero | `0` | Página, desde 0 |
| `size` | entero | `20` | Tamaño, máximo 100 |

Ejemplo:

```http
GET /api/reportes?estado=PENDIENTE&categoria=FUGA_AGUA&page=0&size=20
```

Respuesta `200`:

```json
{
  "contenido": [
    {
      "id": 312,
      "estado": "PENDIENTE",
      "categoriaIa": "FUGA_AGUA",
      "severidadIa": "ALTA",
      "prioridadCalculada": "CRITICA",
      "descripcion": "Fuga frente al hospital",
      "claveImagenThumb": "incidentes/.../thumb.jpg",
      "urlImagenThumb": "http://localhost:9000/reportes/...?X-Amz-Signature=...",
      "barrioId": 124,
      "entidadAsignadaId": 2,
      "usuarioId": 124,
      "reportePadreId": null,
      "fechaCreacion": "2026-10-27T18:00:00Z",
      "fechaActualizacion": "2026-10-27T18:00:00Z"
    }
  ],
  "pagina": 0,
  "tamano": 20,
  "total": 1,
  "totalPaginas": 1
}
```

Notas importantes:

- El listado está ordenado por `fechaCreacion` descendente.
- Un funcionario o empresa **no puede** ver reportes de otra entidad. Si envía
  `entidadId` de otra entidad, el backend ignora ese filtro y fuerza la entidad del token.
- Los reportes sin entidad asignada solo los ve el administrador.
- La lista no incluye latitud/longitud; para el mapa usa `/api/reportes/geojson`.

### 6.4 Mis reportes

```http
GET /api/reportes/mios?page=0&size=20
Authorization: Bearer <token>
```

Disponible para cualquier usuario autenticado. Solo acepta `page` y `size`; no acepta
los filtros del listado administrativo.

Devuelve el mismo formato `PaginaResponse<ReporteResumenResponse>` y solo los reportes
cuyo `usuarioId` coincide con el usuario del token.

Uso típico en la app móvil:

```ts
import { apiFetch } from "./client";
import type { PaginaResponse, ReporteResumen } from "../types/api";

const misReportes = await apiFetch<PaginaResponse<ReporteResumen>>(
  "/api/reportes/mios?page=0&size=20"
);
```

### 6.5 Cambiar estado

```http
PATCH /api/reportes/{id}/estado
Authorization: Bearer <token>
Content-Type: application/json
```

Roles: `FUNCIONARIO` o `ADMINISTRADOR`.

Body:

```json
{
  "estado": "EN_REVISION"
}
```

Valores permitidos:

```text
PENDIENTE
EN_REVISION
ASIGNADO
RESUELTO
RECHAZADO
```

Respuesta `200`: el reporte completo actualizado.

Reglas:

- Un funcionario solo puede cambiar el estado de un reporte de su entidad.
- Un administrador puede cambiar cualquiera.
- Un ciudadano recibe `403`.
- No hay historial de estados en esta versión.

### 6.6 Asignar entidad

```http
PATCH /api/reportes/{id}/asignacion
Authorization: Bearer <token>
Content-Type: application/json
```

Solo `ADMINISTRADOR`.

Body:

```json
{
  "entidadId": 2
}
```

Respuesta `200`: el reporte completo con la nueva `entidadAsignadaId`.

La asignación **no cambia el estado**. Si quieres marcar el reporte como asignado,
haz una segunda petición a `/estado` con `ASIGNADO`.

### 6.7 GeoJSON de reportes

```http
GET /api/reportes/geojson
Authorization: Bearer <token>
```

Roles: `FUNCIONARIO`, `ADMINISTRADOR` o `EMPRESA`.

Acepta los mismos filtros del listado **excepto** `page` y `size`:

```text
estado, categoria, prioridad, barrioId, entidadId, desde, hasta, texto
```

Devuelve como máximo **2000 reportes**, ordenados por fecha descendente.

Respuesta `200` con `Content-Type: application/geo+json`:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [-76.532, 3.4516]
      },
      "properties": {
        "id": 312,
        "estado": "PENDIENTE",
        "categoria": "FUGA_AGUA",
        "prioridad": "CRITICA",
        "thumbUrl": "http://localhost:9000/reportes/...?X-Amz-Signature=...",
        "fechaCreacion": "2026-10-27T18:00:00Z"
      }
    }
  ]
}
```

Las coordenadas están en orden **longitud, latitud** (`[lng, lat]`), como exige GeoJSON.

---

## 7. Dashboard

### 7.1 Resumen

```http
GET /api/dashboard/resumen
Authorization: Bearer <token>
```

Roles: `FUNCIONARIO`, `ADMINISTRADOR` o `EMPRESA`.

Filtros opcionales:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `desde` | `YYYY-MM-DD` | Inicio del rango, inclusive |
| `hasta` | `YYYY-MM-DD` | Fin del rango, inclusive |
| `barrioId` | entero | Limita a un barrio |

El alcance por entidad se aplica automáticamente. El administrador ve toda la ciudad.

Respuesta `200`:

```json
{
  "total": 42,
  "porEstado": {
    "PENDIENTE": 20,
    "EN_REVISION": 8,
    "ASIGNADO": 7,
    "RESUELTO": 5,
    "RECHAZADO": 2
  },
  "porCategoria": {
    "HUECO_VIA": 15,
    "FUGA_AGUA": 12,
    "FALLA_ALUMBRADO": 9,
    "ACUMULACION_BASURA": 6
  },
  "porPrioridad": {
    "BAJA": 10,
    "MEDIA": 18,
    "ALTA": 11,
    "CRITICA": 3
  },
  "criticos": 3,
  "sinAsignar": 0,
  "porBarrio": [
    {
      "barrioId": 124,
      "nombre": "Centro",
      "total": 8
    }
  ]
}
```

Interpretación:

- `total`: total del alcance y filtros aplicados.
- `porEstado`, `porCategoria`, `porPrioridad`: incluyen todas las claves del enum,
  con `0` cuando no hay datos.
- `criticos`: reportes con prioridad `CRITICA`.
- `sinAsignar`: reportes con `entidadAsignadaId = null`. Para un funcionario o empresa
  normalmente será 0 porque su alcance solo incluye reportes asignados.
- `porBarrio`: hasta los 10 barrios con más reportes.

---

## 8. Catálogos y mapas

### 8.1 Entidades

```http
GET /api/entidades
Authorization: Bearer <token>
```

Roles: `FUNCIONARIO`, `ADMINISTRADOR` o `EMPRESA`.

Respuesta `200`:

```json
[
  {
    "id": 1,
    "nombre": "Secretaria de Obras Publicas",
    "tipo": "SECRETARIA_OBRAS_PUBLICAS",
    "contactoEmail": "obras@municipio.gov.co"
  },
  {
    "id": 2,
    "nombre": "Empresa de Acueducto y Alcantarillado",
    "tipo": "EMPRESA_SERVICIOS_PUBLICOS",
    "contactoEmail": "acueducto@empresa.gov.co"
  }
]
```

Se usa para:

- mostrar filtros por entidad al administrador;
- mostrar el catálogo en el formulario de asignación;
- mostrar el nombre de la entidad en la tabla.

### 8.2 Barrios

```http
GET /api/barrios/geojson
Authorization: Bearer <token>
```

Roles: `FUNCIONARIO`, `ADMINISTRADOR` o `EMPRESA`.

Respuesta `200`, `Content-Type: application/geo+json`:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Polygon",
        "coordinates": [[[-76.54, 3.44], [-76.53, 3.44], "..."]]
      },
      "properties": {
        "gid": 124,
        "nombre": "Centro",
        "codComuna": "01",
        "codBarrio": "001"
      }
    }
  ]
}
```

La geometría puede ser polígono o multipolígono. No asumas que siempre es un punto.

### 8.3 Sitios de interés

```http
GET /api/sitios-interes/geojson
Authorization: Bearer <token>
```

Roles: `FUNCIONARIO`, `ADMINISTRADOR` o `EMPRESA`.

Respuesta `200`, `Content-Type: application/geo+json`:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [-75.607, 1.615]
      },
      "properties": {
        "gid": 7,
        "nombre": "Hospital Departamental",
        "tipoSitio": "HOSPITAL",
        "radioInfluencia": 300,
        "critico": true
      }
    }
  ]
}
```

La geometría puede ser punto, línea o polígono según el sitio. `critico` indica si el
tipo está dentro de los tipos críticos de la ontología.

---

## 9. Tipos TypeScript

Copia estos tipos en un archivo como `src/types/api.ts`. Están alineados con los DTOs
del backend.

```ts
// ---------- Enums ----------

export type RolUsuario =
  | "CIUDADANO"
  | "FUNCIONARIO"
  | "ADMINISTRADOR"
  | "EMPRESA";

export type EstadoReporte =
  | "PENDIENTE"
  | "EN_REVISION"
  | "ASIGNADO"
  | "RESUELTO"
  | "RECHAZADO";

export type TipoIncidencia =
  | "HUECO_VIA"
  | "FUGA_AGUA"
  | "FALLA_ALUMBRADO"
  | "ACUMULACION_BASURA";

export type Severidad = "BAJA" | "MEDIA" | "ALTA" | "CRITICA";

export type PrioridadCalculada = "BAJA" | "MEDIA" | "ALTA" | "CRITICA";

export type TipoEntidad =
  | "SECRETARIA_OBRAS_PUBLICAS"
  | "EMPRESA_SERVICIOS_PUBLICOS"
  | "EMPRESA_ASEO";

// ---------- Autenticación ----------

export interface RegistroRequest {
  nombre: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UsuarioResumen {
  id: number;
  nombre: string;
  email: string;
  rol: RolUsuario;
  entidadId: number | null;
}

export interface TokenResponse {
  token: string;
  tipo: "Bearer" | string;
  expiraEnSegundos: number;
  usuario: UsuarioResumen;
}

// ---------- Archivos ----------

export interface ArchivoResponse {
  clave: string;
  url: string;
  claveThumb: string | null;
  urlThumb: string | null;
}

// ---------- Reportes ----------

export interface ReporteCreacionRequest {
  descripcion?: string | null;
  categoriaIa?: TipoIncidencia | null;
  severidadIa?: Severidad | null;
  latitud: number;
  longitud: number;
  claveImagenHd: string;
  claveImagenThumb?: string | null;
}

export interface ReporteResponse {
  id: number;
  estado: EstadoReporte;
  categoriaIa: TipoIncidencia | null;
  severidadIa: Severidad | null;
  prioridadCalculada: PrioridadCalculada | null;
  confianzaIa: number | null;
  descripcion: string | null;
  latitud: number;
  longitud: number;
  claveImagenHd: string;
  urlImagenHd: string | null;
  claveImagenThumb: string | null;
  urlImagenThumb: string | null;
  fechaCreacion: string;
  fechaActualizacion: string;
  usuarioId: number;
  barrioId: number | null;
  entidadAsignadaId: number | null;
  reportePadreId: number | null;
}

export interface ReporteResumen {
  id: number;
  estado: EstadoReporte;
  categoriaIa: TipoIncidencia | null;
  severidadIa: Severidad | null;
  prioridadCalculada: PrioridadCalculada | null;
  descripcion: string | null;
  claveImagenThumb: string | null;
  urlImagenThumb: string | null;
  barrioId: number | null;
  entidadAsignadaId: number | null;
  usuarioId: number;
  reportePadreId: number | null;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface PaginaResponse<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  total: number;
  totalPaginas: number;
}

export interface CambioEstadoRequest {
  estado: EstadoReporte;
}

export interface CambioAsignacionRequest {
  entidadId: number;
}

// ---------- Dashboard ----------

export interface ConteoBarrio {
  barrioId: number;
  nombre: string;
  total: number;
}

export interface DashboardResumen {
  total: number;
  porEstado: Record<EstadoReporte, number>;
  porCategoria: Record<TipoIncidencia, number>;
  porPrioridad: Record<PrioridadCalculada, number>;
  criticos: number;
  sinAsignar: number;
  porBarrio: ConteoBarrio[];
}

// ---------- Catálogos ----------

export interface EntidadResumen {
  id: number;
  nombre: string;
  tipo: TipoEntidad;
  contactoEmail: string;
}

// ---------- GeoJSON ----------

export interface GeoJsonGeometry {
  type: string;
  coordinates: unknown;
}

export interface GeoJsonFeature<P> {
  type: "Feature";
  geometry: GeoJsonGeometry;
  properties: P;
}

export interface FeatureCollection<P> {
  type: "FeatureCollection";
  features: Array<GeoJsonFeature<P>>;
}

export interface ReporteGeoProperties {
  id: number;
  estado: EstadoReporte;
  categoria: TipoIncidencia | null;
  prioridad: PrioridadCalculada | null;
  thumbUrl: string | null;
  fechaCreacion: string;
}

export interface BarrioGeoProperties {
  gid: number;
  nombre: string;
  codComuna: string;
  codBarrio: string;
}

export interface SitioGeoProperties {
  gid: number;
  nombre: string;
  tipoSitio: string;
  radioInfluencia: number;
  critico: boolean;
}

export type ReporteFeatureCollection = FeatureCollection<ReporteGeoProperties>;
export type BarrioFeatureCollection = FeatureCollection<BarrioGeoProperties>;
export type SitioFeatureCollection = FeatureCollection<SitioGeoProperties>;

// Variante específica para los reportes, que siempre son puntos.
export interface ReporteGeoFeature {
  type: "Feature";
  geometry: {
    type: "Point";
    coordinates: [number, number]; // [longitud, latitud]
  };
  properties: ReporteGeoProperties;
}

export interface ReportePuntoFeatureCollection {
  type: "FeatureCollection";
  features: ReporteGeoFeature[];
}

// ---------- Errores ----------

export interface ErrorApi {
  error: string;
  mensaje?: string;
  campos?: Record<string, string>;
}
```

### Tipado específico del punto de un reporte

El tipo `ReportePuntoFeatureCollection` incluido arriba es el que conviene usar en el
mapa móvil, porque garantiza que `coordinates` es una tupla `[longitud, latitud]`.
Para barrios y sitios puedes usar `BarrioFeatureCollection` y `SitioFeatureCollection`,
cuya geometría es más general.

---

## 10. Cliente HTTP recomendado

Puedes crear un archivo `src/api/client.ts`:

```ts
import type { ErrorApi } from "../types/api";

const API_BASE =
  import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export class ApiHttpError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: ErrorApi
  ) {
    super(body.error ?? `HTTP ${status}`);
  }
}

let token: string | null = null;

export function setAuthToken(nuevoToken: string | null) {
  token = nuevoToken;
}

export function getAuthToken() {
  return token;
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers);

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const esFormData = options.body instanceof FormData;
  if (options.body && !esFormData && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const respuesta = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!respuesta.ok) {
    const body = (await respuesta.json().catch(() => ({
      error: `HTTP ${respuesta.status}`,
    }))) as ErrorApi;

    throw new ApiHttpError(respuesta.status, body);
  }

  if (respuesta.status === 204) {
    return undefined as T;
  }

  return (await respuesta.json()) as T;
}
```

> En React Native reemplaza el manejo de `token` por `expo-secure-store`.
> En web, si usas `localStorage`, inicializa el token al arrancar la app.

### 10.1 Login

```ts
import type { TokenResponse } from "../types/api";
import { apiFetch, setAuthToken } from "./client";

export async function login(email: string, password: string) {
  const respuesta = await apiFetch<TokenResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

  setAuthToken(respuesta.token);
  // En web: localStorage.setItem("token", respuesta.token);
  // En móvil: await SecureStore.setItemAsync("token", respuesta.token);

  return respuesta;
}
```

### 10.2 Listar reportes con filtros

```ts
import type { PaginaResponse, ReporteResumen } from "../types/api";
import { apiFetch } from "./client";

export interface FiltrosReportes {
  estado?: string;
  categoria?: string;
  prioridad?: string;
  barrioId?: number;
  entidadId?: number;
  desde?: string;
  hasta?: string;
  texto?: string;
  page?: number;
  size?: number;
}

export function listarReportes(filtros: FiltrosReportes) {
  const params = new URLSearchParams();

  Object.entries(filtros).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== "") {
      params.set(clave, String(valor));
    }
  });

  return apiFetch<PaginaResponse<ReporteResumen>>(
    `/api/reportes?${params.toString()}`
  );
}
```

### 10.3 Subir imagen y crear reporte

```ts
import type { ArchivoResponse, ReporteResponse } from "../types/api";
import { apiFetch } from "./client";

export async function subirImagen(file: File) {
  const formData = new FormData();
  formData.append("archivo", file);

  return apiFetch<ArchivoResponse>("/api/archivos", {
    method: "POST",
    body: formData,
  });
}

export async function crearReporte(params: {
  archivo: ArchivoResponse;
  latitud: number;
  longitud: number;
  descripcion?: string;
}) {
  return apiFetch<ReporteResponse>("/api/reportes", {
    method: "POST",
    body: JSON.stringify({
      descripcion: params.descripcion,
      latitud: params.latitud,
      longitud: params.longitud,
      claveImagenHd: params.archivo.clave,
      claveImagenThumb: params.archivo.claveThumb,
    }),
  });
}
```

### 10.4 Dashboard

```ts
import type { DashboardResumen } from "../types/api";
import { apiFetch } from "./client";

export function cargarResumen(desde?: string, hasta?: string) {
  const params = new URLSearchParams();
  if (desde) params.set("desde", desde);
  if (hasta) params.set("hasta", hasta);

  return apiFetch<DashboardResumen>(
    `/api/dashboard/resumen?${params.toString()}`
  );
}
```

### 10.5 Cambiar estado y asignar

```ts
import type { EstadoReporte, ReporteResponse } from "../types/api";
import { apiFetch } from "./client";

export function cambiarEstado(id: number, estado: EstadoReporte) {
  return apiFetch<ReporteResponse>(`/api/reportes/${id}/estado`, {
    method: "PATCH",
    body: JSON.stringify({ estado }),
  });
}

export function asignarEntidad(id: number, entidadId: number) {
  return apiFetch<ReporteResponse>(`/api/reportes/${id}/asignacion`, {
    method: "PATCH",
    body: JSON.stringify({ entidadId }),
  });
}
```

### 10.6 GeoJSON en un mapa web con Leaflet

```ts
import L from "leaflet";
import type { ReporteFeatureCollection } from "../types/api";
import { apiFetch } from "./client";

export async function cargarReportesEnMapa(map: L.Map) {
  const datos = await apiFetch<ReporteFeatureCollection>(
    "/api/reportes/geojson"
  );

  L.geoJSON(datos as any, {
    pointToLayer: (_feature, latlng) =>
      L.circleMarker(latlng, {
        radius: 6,
        color: "#b91c1c",
        fillOpacity: 0.8,
      }),
  }).addTo(map);
}
```

### 10.7 GeoJSON en React Native Maps

```tsx
import MapView, { Marker } from "react-native-maps";
import { useEffect, useState } from "react";
import type { ReportePuntoFeatureCollection } from "../types/api";
import { apiFetch } from "./client";

export function MapaReportes() {
  const [datos, setDatos] = useState<ReportePuntoFeatureCollection | null>(null);

  useEffect(() => {
    apiFetch<ReportePuntoFeatureCollection>("/api/reportes/geojson").then(setDatos);
  }, []);

  if (!datos) return null;

  return (
    <MapView style={{ flex: 1 }}>
      {datos.features.map((feature) => {
        const [longitude, latitude] = feature.geometry.coordinates;
        return (
          <Marker
            key={feature.properties.id}
            coordinate={{ latitude, longitude }}
            title={feature.properties.categoria ?? undefined}
          />
        );
      })}
    </MapView>
  );
}
```

---

## 11. Flujos recomendados por pantalla

### 11.1 App móvil del ciudadano

```text
Registro/Login
  → guardar token
  → seleccionar foto
  → POST /api/archivos
  → POST /api/reportes con claves + latitud/longitud
  → mostrar confirmación
  → GET /api/reportes/mios
  → GET /api/reportes/{id}
```

### 11.2 Dashboard del funcionario o empresa

```text
Login
  → GET /api/reportes
  → GET /api/dashboard/resumen
  → GET /api/reportes/geojson
  → GET /api/barrios/geojson
  → GET /api/sitios-interes/geojson
  → PATCH /api/reportes/{id}/estado
```

### 11.3 Dashboard del administrador

```text
Login
  → GET /api/entidades
  → GET /api/reportes?entidadId=...
  → GET /api/dashboard/resumen
  → PATCH /api/reportes/{id}/asignacion
  → PATCH /api/reportes/{id}/estado
```

### 11.4 Actualización de datos

La API no expone WebSockets ni eventos en tiempo real. Después de crear un reporte,
cambiar su estado o reasignar su entidad, vuelve a consultar el listado, el detalle,
las métricas o el GeoJSON para reflejar el cambio.

### 11.5 Matriz de permisos por pantalla

| Pantalla | Ciudadano | Funcionario | Empresa | Administrador |
|---|:---:|:---:|:---:|:---:|
| Registro/Login | ✅ | ✅ | ✅ | ✅ |
| Crear reporte | ✅ | ✅ | ✅ | ✅ |
| Mis reportes | ✅ | ✅ | ✅ | ✅ |
| Listado general | ❌ | ✅ | ✅ | ✅ |
| Detalle de cualquier reporte | Solo propios | Solo su entidad | Solo su entidad | ✅ |
| Cambiar estado | ❌ | ✅ | ❌ | ✅ |
| Asignar entidad | ❌ | ❌ | ❌ | ✅ |
| Métricas | ❌ | ✅ | ✅ | ✅ |
| Mapa | ❌ | ✅ | ✅ | ✅ |
| Catálogo de entidades | ❌ | ✅ | ✅ | ✅ |

---

## 12. Problemas frecuentes y cómo resolverlos

### “Me devuelve 401”

- No enviaste `Authorization`.
- El token expiró (120 minutos por defecto).
- El token es inválido.
- El usuario cambió de rol o contraseña y el token viejo ya no es coherente.

Solución: volver al login y guardar el token nuevo.

### “Me devuelve 403”

- El rol no tiene permiso para ese endpoint.
- El funcionario no tiene entidad asignada.
- El reporte no pertenece a la entidad del funcionario.
- El ciudadano intenta ver un reporte que no es suyo.
- El ciudadano intenta entrar a `/api/reportes` o al dashboard.

Solución: revisar el rol y el alcance. Un ciudadano debe usar `/api/reportes/mios` y
`/api/reportes/{id}` solo para sus propios reportes.

### “Me devuelve 400 con `campos`”

Es una validación de formulario. Revisa el nombre del campo y muestra el mensaje
correspondiente. Ejemplo: latitud fuera de rango, imagen vacía o contraseña corta.

### “Me devuelve 400 con `Solicitud malformada`”

El JSON tiene un enum inválido, un tipo incorrecto o un campo que no existe en el DTO.
Verifica mayúsculas y valores exactos.

### “Me devuelve 413 al subir la imagen”

La imagen supera el máximo permitido (10 MB). Comprímela o pide otra.

### “La imagen no carga”

- En modo local, la URL `/api/archivos/...` requiere token. No la uses directamente en
  un `<img>` sin autenticación.
- En un dispositivo físico, `localhost` apunta al teléfono. Usa la IP LAN del computador.
- En S3, la URL firmada expira (15 minutos por defecto). Solicita el reporte de nuevo.
- Configura `gestion.almacenamiento.url-base` o el endpoint S3 con un host accesible
  desde el dispositivo.

### “CORS error en el navegador”

- El origen del frontend no está en `gestion.cors.origenes-permitidos`.
- Cambió el puerto de Vite o Expo.
- El backend no se reinició después de cambiar la configuración.

Agrega el origen exacto, por ejemplo:

```properties
gestion.cors.origenes-permitidos=http://localhost:5173,http://localhost:8081
```

### “El funcionario ve reportes vacíos”

Puede que no tenga `entidad_id` asignado o que no haya reportes asignados a su entidad.
Verifica en base de datos y vuelve a iniciar sesión.

### “El mapa muestra muchos puntos repetidos”

La deduplicación enlaza reportes cercanos con `reportePadreId`, pero el GeoJSON devuelve
todos los reportes y **no incluye `reportePadreId` en `properties`**. Por eso, en esta
versión no se pueden filtrar solo los reportes raíz desde el GeoJSON.

Opciones:

- mostrar todos los puntos y agruparlos visualmente con la librería de mapas;
- usar el listado paginado para identificar los reportes raíz;
- ampliar el DTO del GeoJSON para incluir `reportePadreId` en una fase posterior.

---

## 13. Checklist de integración

- [ ] Configurar `API_BASE` según web, emulador o dispositivo físico.
- [ ] Implementar login y guardar el token de forma segura.
- [ ] Agregar el header `Authorization` a todas las peticiones protegidas.
- [ ] Manejar `401` volviendo al login.
- [ ] Manejar `403` mostrando un mensaje de permisos.
- [ ] Implementar los formularios de registro y creación de reporte.
- [ ] Subir la imagen con `FormData` usando el campo `archivo`.
- [ ] Enviar `claveImagenHd` y, si existe, `claveImagenThumb` al crear el reporte.
- [ ] No enviar `usuarioId` al crear un reporte.
- [ ] Usar `/api/reportes/mios` en la app móvil.
- [ ] Usar `/api/reportes` en el dashboard de funcionarios y administradores.
- [ ] Cargar las métricas con `/api/dashboard/resumen`.
- [ ] Cargar el mapa con `/api/reportes/geojson`, barrios y sitios.
- [ ] Mostrar el nombre de la entidad usando `/api/entidades`.
- [ ] Implementar cambio de estado y asignación según el rol.
- [ ] Probar con tokens de cada rol.
- [ ] Revisar Swagger para confirmar el contrato actualizado.

---

## 14. Referencias

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Documentación del dashboard: `docs/dashboard-api.md`
- Documentación de seguridad: `docs/seguridad.md`
- Documentación de almacenamiento: `docs/almacenamiento.md`
