# API del dashboard

**Módulo:** consulta, métricas, mapas y asignación de reportes
**Estado:** implementado y cubierto por pruebas de integración
**Última actualización:** octubre de 2026

---

## 1. Qué resuelve

El dashboard necesita tres cosas distintas del backend:

1. **Listar y filtrar** reportes para la tabla de trabajo.
2. **Resumir** el estado de la ciudad para las tarjetas y gráficas.
3. **Pintar mapas** con reportes, barrios y sitios de interés.

Además, el administrador puede **reasignar** un reporte a otra entidad. La app móvil
usa la misma API para mostrar “mis reportes”.

---

## 2. Reglas de acceso

| Rol | Alcance |
|---|---|
| `ADMINISTRADOR` | Ve y gestiona todos los reportes. Puede filtrar por cualquier entidad y reasignar. |
| `FUNCIONARIO` | Solo ve reportes **asignados a su entidad**. Si no tiene entidad, recibe `403`. |
| `EMPRESA` | Igual que funcionario: solo su entidad. |
| `CIUDADANO` | No usa el dashboard. Puede consultar sus propios reportes en `/api/reportes/mios` y el detalle de los suyos. |

Reglas concretas:

- `GET /api/reportes`, `GET /api/reportes/geojson` y `GET /api/dashboard/resumen`:
  `FUNCIONARIO`, `ADMINISTRADOR` o `EMPRESA`.
- `GET /api/reportes/mios`: cualquier usuario autenticado; devuelve solo sus reportes.
- `GET /api/reportes/{id}`: el dueño si es ciudadano; la entidad asignada si es
  funcionario/empresa; cualquier reporte si es administrador.
- `PATCH /api/reportes/{id}/estado`: `FUNCIONARIO` o `ADMINISTRADOR`, respetando el
  alcance por entidad.
- `PATCH /api/reportes/{id}/asignacion`: solo `ADMINISTRADOR`.

Los catálogos (`barrios`, `sitios-interes`, `entidades`) son de consulta para los roles
del dashboard.

---

## 3. Listado paginado

### `GET /api/reportes`

Filtros opcionales:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `estado` | enum | `PENDIENTE`, `EN_REVISION`, `ASIGNADO`, `RESUELTO`, `RECHAZADO` |
| `categoria` | enum | `HUECO_VIA`, `FUGA_AGUA`, `FALLA_ALUMBRADO`, `ACUMULACION_BASURA` |
| `prioridad` | enum | `BAJA`, `MEDIA`, `ALTA`, `CRITICA` |
| `barrioId` | entero | `gid` del barrio |
| `entidadId` | entero | Solo el administrador puede usarlo; para los demás se fuerza su entidad |
| `desde` | fecha ISO | `YYYY-MM-DD`, inclusive desde las 00:00 |
| `hasta` | fecha ISO | `YYYY-MM-DD`, inclusive durante todo ese día |
| `texto` | texto | Búsqueda parcial, sin distinguir mayúsculas, en la descripción |
| `page` | entero | Desde 0, por defecto 0 |
| `size` | entero | Por defecto 20, máximo 100 |

Orden: `fechaCreacion` descendente.

Ejemplo:

```http
GET /api/reportes?estado=PENDIENTE&categoria=FUGA_AGUA&page=0&size=20
Authorization: Bearer <token>
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
      "claveImagenThumb": "incidentes/2026/10/27/.../thumb.jpg",
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

La URL de la miniatura se resuelve en cada respuesta: con almacenamiento local apunta al
backend y con S3 es una URL firmada temporal. En la base de datos solo se guarda la clave.

### `GET /api/reportes/mios`

Mismos parámetros de paginación (`page`, `size`) y mismo formato de respuesta. Devuelve
los reportes cuyo `usuarioId` coincide con el usuario del token. Es el endpoint que usa la
app móvil para la sección “Mis reportes”.

---

## 4. Métricas del dashboard

### `GET /api/dashboard/resumen`

Filtros opcionales:

- `desde` (`YYYY-MM-DD`)
- `hasta` (`YYYY-MM-DD`)
- `barrioId`

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
    { "barrioId": 124, "nombre": "Centro", "total": 8 }
  ]
}
```

Detalles:

- Los mapas `porEstado`, `porCategoria` y `porPrioridad` incluyen **todas** las claves
  del enum, con `0` cuando no hay reportes.
- `criticos` cuenta los reportes con `prioridadCalculada = CRITICA`.
- `sinAsignar` cuenta los que tienen `entidadAsignadaId = null`.
- `porBarrio` devuelve como máximo los **10 barrios con más reportes**.
- `total` es el total del alcance y los filtros aplicados, no una suma de los mapas si
  existen datos históricos con campos nulos.

---

## 5. GeoJSON

### `GET /api/reportes/geojson`

Acepta los mismos filtros que el listado, excepto `page` y `size`. Devuelve como máximo
**2000 reportes**, ordenados por fecha descendente.

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

Las coordenadas se transforman de **EPSG:9377** a **EPSG:4326** (longitud, latitud)
directamente en PostGIS.

### `GET /api/barrios/geojson`

`FeatureCollection` con los 180 barrios. Cada `properties` incluye `gid`, `nombre`,
`codComuna` y `codBarrio`.

### `GET /api/sitios-interes/geojson`

`FeatureCollection` con los sitios de interés. Cada `properties` incluye `gid`, `nombre`,
`tipoSitio`, `radioInfluencia` y `critico`.

Los tres endpoints responden con `Content-Type: application/geo+json`.

---

## 6. Catálogo de entidades

### `GET /api/entidades`

Devuelve las entidades responsables para poblar filtros y el formulario de asignación:

```json
[
  {
    "id": 1,
    "nombre": "Secretaria de Obras Publicas",
    "tipo": "SECRETARIA_OBRAS_PUBLICAS",
    "contactoEmail": "obras@municipio.gov.co"
  }
]
```

---

## 7. Asignación administrativa

### `PATCH /api/reportes/{id}/asignacion`

Solo `ADMINISTRADOR`.

```json
{
  "entidadId": 2
}
```

Respuesta `200`: el reporte completo (`ReporteResponse`) con la nueva
`entidadAsignadaId`. La asignación **no cambia el estado**; para eso se usa
`PATCH /api/reportes/{id}/estado`.

---

## 8. CORS y OpenAPI

Orígenes permitidos por defecto (configurables en `gestion.cors.origenes-permitidos`):

- `http://localhost:5173` (React/Vite)
- `http://localhost:8081` (Expo)
- `http://localhost:19006` (Expo web)

Métodos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`. Header permitido:
`Authorization`, entre otros.

Documentación interactiva:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Ambas rutas son públicas; el resto de `/api/**` exige token.

---

## 9. Implementación

| Archivo | Rol |
|---|---|
| `controlador/ReporteController.java` | Listado, mis reportes, GeoJSON y asignación |
| `controlador/DashboardController.java` | Resumen de métricas |
| `controlador/CatalogoController.java` | GeoJSON de barrios/sitios y entidades |
| `servicio/ReporteService.java` | Filtros, paginación y alcance por entidad |
| `servicio/DashboardService.java` | Agregaciones nativas y GeoJSON de reportes |
| `servicio/CatalogoService.java` | GeoJSON de catálogos y entidades |
| `seguridad/AlcanceReportes.java` | Reglas de visibilidad por rol y entidad |
| `repositorio/ReporteEspecificaciones.java` | Filtros dinámicos con JPA Criteria |
| `dto/ReporteResumenResponse.java` | Fila de tabla del dashboard |
| `dto/DashboardResumenResponse.java` | Tarjetas y gráficas |
| `dto/FeatureCollection.java` | Respuesta GeoJSON de reportes |

---

## 10. Pruebas y verificación

`DashboardApiTest` cubre:

- Ciudadano bloqueado en listado y resumen (`403`).
- Funcionario limitado a su entidad, incluso si intenta filtrar por otra.
- Listado paginado y filtro por categoría del administrador.
- `/api/reportes/mios` solo devuelve reportes del token.
- Coherencia de `total` con la suma de `porEstado`.
- GeoJSON de reportes con coordenadas válidas.
- Catálogos GeoJSON y entidades.
- Asignación por administrador; funcionario recibe `403`.
- Preflight CORS para el origen del dashboard.
- OpenAPI disponible sin token.

Resultado de la suite: **36/36 pruebas en verde** con `./mvnw clean test`.

---

## 11. Limitaciones actuales

- No hay historial completo de cambios de estado; se difirió para una fase posterior.
- La asignación no dispara notificaciones ni correos.
- El GeoJSON de reportes limita a 2000 features para mantener el mapa fluido; si se
  necesita más, se debe implementar clustering o paginación geográfica.
- La app móvil debe usar `exp://` o la IP de la máquina en Expo Go; CORS solo aplica a
  clientes web, no a peticiones nativas.
