# Almacenamiento de archivos (fotos)

**Módulo:** Object storage S3-compatible para las fotos de los reportes
**Estado:** adaptador local (dev) + adaptador S3/MinIO implementados
**Dependencias:** AWS SDK v2 (`software.amazon.awssdk:s3:2.57.3`), Silo (contenedor, API S3)

> **Nota de contexto (2026):** MinIO dejó de publicar sus imágenes comunitarias en Docker Hub
> y Quay. Para el entorno local usamos **Silo** (`pgsty/silo`), un fork comunitario de MinIO
> con el mismo CLI y la misma API S3. El código del backend usa el **AWS SDK v2 estándar**,
> por lo que funciona igual contra Silo, MinIO, LocalStack o **Amazon S3**.

---

## 1. Por qué object storage

Las fotos **no** se guardan en PostgreSQL ni en el disco del servidor:

- En PostgreSQL (`BYTEA`) inflarían las filas, los backups y las consultas.
- En disco local, los contenedores/instancias son efímeros y no se comparte entre servidores.

El object storage guarda archivos como **objetos** dentro de **buckets**, accesibles por HTTP.
La base de datos guarda solo la **clave** del objeto (`incidentes/2026/10/24/<uuid>/hd.jpg`).

---

## 2. Arquitectura: puerto y adaptadores

```text
AlmacenamientoArchivos  (interfaz)
├── AlmacenamientoLocal   @Profile("dev & !minio")   → disco, sin Docker
└── AlmacenamientoMinio   @Profile("minio")          → S3 (Silo / MinIO / AWS) con AWS SDK v2
```

| Archivo | Rol |
|---|---|
| `almacenamiento/AlmacenamientoArchivos.java` | Puerto: `guardar`, `urlDe`, `obtener` |
| `almacenamiento/AlmacenamientoLocal.java` | Adaptador local (`uploads/`, ignorado por git) |
| `almacenamiento/AlmacenamientoMinio.java` | Adaptador S3 con **URLs firmadas** (bucket privado) |
| `almacenamiento/RecursoAlmacenado.java` | Contenido + content type + tamaño |
| `servicio/ServicioArchivos.java` | Validación, generación de clave y orquestación |
| `controlador/ArchivoController.java` | Endpoints REST |

`ReporteService` solo conoce la interfaz: guarda la clave y resuelve la URL al responder.

---

## 3. API

> **Autenticación (fase de seguridad):** todos los endpoints de esta sección requieren
> `Authorization: Bearer <token>`; solo `/api/auth/**` es público. Ver `docs/seguridad.md`.

### Subir una imagen (multipart)

```http
POST /api/archivos
Content-Type: multipart/form-data
campo: archivo
```

Validaciones: JPEG/PNG/WEBP · máximo 10 MB · la clave la genera el servidor.

Respuesta `201 Created`:

```json
{
  "clave": "incidentes/2026/10/24/1e841516-d57b-4db3-bb2c-856d2d633fbd/hd.jpg",
  "url": "http://localhost:9000/reportes/incidentes/.../hd.jpg?X-Amz-Signature=..."
}
```

- Con almacenamiento local, `url` apunta al backend: `http://localhost:8080/api/archivos/<clave>`.
- Con S3 (Silo/MinIO/AWS), `url` es una **URL firmada temporal** (15 min por defecto) de un
  bucket **privado**.

### Descargar (solo adaptador local / proxy)

```http
GET /api/archivos/{clave}
```

Con S3, los clientes usan directamente la URL firmada devuelta por la API.

### Crear un reporte con la clave

```http
POST /api/reportes
{
  "categoriaIa": "FUGA_AGUA",
  "severidadIa": "ALTA",
  "latitud": 1.619537,
  "longitud": -75.611134,
  "claveImagenHd": "incidentes/2026/10/24/<uuid>/hd.jpg",
  "usuarioId": 5
}
```

La respuesta incluye `claveImagenHd`, `urlImagenHd` (resuelta) y `urlImagenThumb` (null hasta
la fase de miniaturas).

> Nota: el campo antes se llamaba `urlImagenHd`. Ahora el contrato usa `claveImagenHd`.
> Los registros históricos que contienen una URL completa (`http...`) se devuelven tal cual.

---

## 4. Perfiles y configuración

| Perfil | Almacenamiento |
|---|---|
| `dev` (por defecto) | Local (`uploads/`) — no requiere Docker |
| `dev,minio` | S3 local (Silo) — requiere Docker y el bucket `reportes` |
| producción | Mismo adaptador S3 con endpoint/credenciales de AWS S3 |

`application-dev.properties` (ignorado por git) contiene:

```properties
gestion.almacenamiento.minio.endpoint=http://localhost:9000
gestion.almacenamiento.minio.bucket=reportes
gestion.almacenamiento.minio.region=us-east-1
gestion.almacenamiento.minio.access-key=${MINIO_ROOT_USER:minioadmin}
gestion.almacenamiento.minio.secret-key=${MINIO_ROOT_PASSWORD:minioadmin}
gestion.almacenamiento.minio.expiracion-url-minutos=15
```

Límites multipart (en `application.properties`):

```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=12MB
```

---

## 5. Object storage local (Silo, API S3)

Archivos: `compose.yaml`, `.env.example` (copia a `.env`, ignorado por git).

```bash
# 1) Iniciar Docker Desktop
# 2) Levantar el almacenamiento + creacion automatica del bucket privado
docker compose up -d

# Consola web: http://localhost:9001  (usuario/clave del .env o minioadmin)
# API S3:     http://localhost:9000
```

El servicio `s3-init` (AWS CLI) espera al servidor, crea el bucket `reportes` y lo deja
**privado** (sin política pública). Es idempotente.

### Arrancar el backend con S3 local

```bash
# Git Bash
SPRING_PROFILES_ACTIVE=dev,minio ./mvnw spring-boot:run

# PowerShell
$env:SPRING_PROFILES_ACTIVE="dev,minio"; ./mvnw spring-boot:run
```

Si el bucket no existe o el servidor no está arriba, la app **falla al arrancar** con un
mensaje claro.

### Prueba manual

```bash
# Subir una imagen real
curl -X POST http://localhost:8080/api/archivos -F "archivo=@C:\ruta\foto.jpg"
# → { clave, url firmada }

# Abrir la URL firmada en el navegador (debe mostrar la imagen)
# Probar la URL SIN firma (http://localhost:9000/reportes/<clave>) → 403
```

---

## 6. Decisiones y limitaciones

- **Bucket privado + URLs firmadas**: no hay acceso anónimo; la URL expira (15 min).
  El dashboard debe pedir el reporte de nuevo para obtener una URL fresca (o usar la clave).
- **La BD guarda la clave**, no la URL: permite cambiar de servidor S3 sin migrar datos.
- **Miniaturas**: pendientes (fase C3) con `Thumbnailator`; requieren imágenes reales en tests.
- **Validación de contenido**: hoy se valida MIME declarado y tamaño; una validación por
  *magic bytes* queda como mejora.
- **Cliente móvil en otro dispositivo**: `localhost` no resuelve; configura el endpoint con la
  IP LAN del PC (`http://192.168.x.x:9000`) para que las URLs firmadas sean alcanzables.
- **Producción (AWS S3)**: mismo adaptador; se cambian endpoint/región/credenciales (IAM) y
  se elimina `forcePathStyle` si se usa el endpoint de AWS.
- **Credenciales**: nunca en git. `.env` y `application-dev.properties` están ignorados.

---

## 7. Verificación automatizada

Los tests (`26/26`) usan el perfil `dev` y el adaptador local, por lo que **no requieren
Docker**. `ArchivoTest` cubre: subida y descarga del contenido exacto, formato inválido,
archivo vacío y creación de reporte con la clave subida devolviendo la URL resuelta.
