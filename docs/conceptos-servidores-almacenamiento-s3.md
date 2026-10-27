# Servidores, almacenamiento de objetos y S3 — Guía conceptual

**Proyecto:** Plataforma Inteligente de Gestión de Incidencias Urbanas
**Uso:** material de consulta para entender la infraestructura del backend
**Última actualización:** octubre de 2026

---

## Índice

1. [La palabra "servidor"](#1-la-palabra-servidor)
2. [Cliente-servidor y puertos](#2-cliente-servidor-y-puertos)
3. [Tipos de servidores](#3-tipos-de-servidores)
4. [¿Qué es un servidor de almacenamiento de objetos?](#4-qué-es-un-servidor-de-almacenamiento-de-objetos)
5. [¿Qué es S3? AWS vs. el estándar](#5-qué-es-s3-aws-vs-el-estándar)
6. [MinIO y Silo](#6-minio-y-silo)
7. [¿Qué es una URL firmada?](#7-qué-es-una-url-firmada)
8. [Cómo encaja todo en este proyecto](#8-cómo-encaja-todo-en-este-proyecto)
9. [Glosario y regla práctica](#9-glosario-y-regla-práctica)

---

## 1. La palabra "servidor"

"Servidor" es una de las palabras más sobrecargadas de la informática. Tiene tres significados que se mezclan:

| Significado | Qué es | Ejemplo |
|---|---|---|
| **Máquina** | Un computador físico (o una VM) dedicado a atender servicios | El VPS donde se despliega el proyecto |
| **Programa/proceso** | Un software que escucha en un **puerto** y responde peticiones | Tomcat (dentro de Spring Boot), PostgreSQL, Silo |
| **Rol/servicio** | "Servidor **de** X": lo que ese programa ofrece | Servidor de aplicaciones, de base de datos, de objetos |

Cuando decimos "levanta el servidor", normalmente hablamos del **programa**. La máquina es solo donde corre.

En desarrollo, **un mismo PC es a la vez cliente y servidor**: el navegador (cliente) habla con Spring Boot (servidor), que a su vez es cliente de PostgreSQL y de Silo.

---

## 2. Cliente-servidor y puertos

- **Cliente**: programa que pide algo (navegador, app móvil, `curl`, o Spring Boot cuando llama a Silo).
- **Servidor**: programa que espera peticiones y responde.
- Se comunican mediante un protocolo: HTTP (API), SQL (base de datos), S3 (objetos), etc.

La máquina tiene una **IP** (dirección del edificio) y cada programa escucha en un **puerto** (número de puerta).

| Puerto | Programa | Rol |
|---|---|---|
| `8080` | Spring Boot (Tomcat embebido) | Servidor de aplicaciones (API) |
| `5432` | PostgreSQL | Servidor de base de datos |
| `9000` | Silo | Servidor de almacenamiento de objetos (API S3) |
| `9001` | Silo (consola) | Interfaz web de administración |
| `9001` (otro uso) | — | No aplica en este proyecto |

Docker ejecuta el programa Silo en un **contenedor aislado** (un proceso con su propio sistema de archivos y red) y publica sus puertos al sistema anfitrión (`-p 9000:9000`). No es una máquina virtual completa: es un proceso empaquetado.

---

## 3. Tipos de servidores

Todos son programas escuchando en un puerto. La diferencia es **qué ofrecen** y con qué **protocolo**:

| Tipo | Qué hace | Protocolo típico | Ejemplo |
|---|---|---|---|
| **Servidor de aplicaciones/web** | Ejecuta lógica de negocio, recibe HTTP y responde JSON/HTML; no guarda archivos grandes | HTTP/REST | Spring Boot (`:8080`) |
| **Servidor de base de datos** | Guarda datos **estructurados** (tablas, filas, relaciones); permite consultas, transacciones, joins | SQL | PostgreSQL (`:5432`) |
| **Servidor de almacenamiento de objetos** | Guarda **archivos completos** (blobs) identificados por clave; no hay consultas ni edición parcial | API S3 (HTTP) | Silo/MinIO (`:9000`), Amazon S3 |
| **Servidor de archivos** | Comparte carpetas con jerarquía, permisos y bloqueos, como un disco de red | SMB/NFS | Un NAS, una carpeta compartida de Windows |
| **Servidor web estático** | Sirve archivos de una carpeta por HTTP | HTTP | Nginx sirviendo un sitio |
| **Servidor DNS** | Traduce nombres a direcciones IP | DNS | El de la red local |

Ninguno es "mejor": cada uno está optimizado para lo suyo. El backend **usa** varios: es servidor para el móvil y cliente de PostgreSQL y de Silo.

---

## 4. ¿Qué es un servidor de almacenamiento de objetos?

Es un programa cuyo único trabajo es **guardar y devolver archivos completos**:

- **Bucket**: contenedor lógico (como una "caja").
- **Objeto**: el archivo (`hd.jpg`) más sus metadatos (tipo, tamaño).
- **Clave (key)**: nombre único dentro del bucket, por ejemplo `incidentes/2026/10/24/<uuid>/hd.jpg`.
- Operaciones: `PUT` (guardar), `GET` (descargar), `DELETE`, `HEAD`. No hay `SELECT`, no hay joins, no hay edición parcial.
- Está optimizado para **durabilidad, escala y servir muchos archivos**, no para consultas.

### Comparación con otros servidores

| Pregunta | Base de datos | Object storage | Servidor de archivos | Servidor de aplicaciones |
|---|---|---|---|---|
| ¿Qué guarda? | Filas y relaciones | Archivos completos + metadatos | Archivos en carpetas | Nada (ejecuta lógica) |
| ¿Cómo se busca? | SQL (`WHERE`, `JOIN`) | Solo por clave exacta | Por ruta | No aplica |
| ¿Se puede editar un fragmento? | Sí (`UPDATE`) | No: se reemplaza el objeto completo | Sí (append/locks) | No aplica |
| ¿Jerarquía de carpetas? | No | No (las `/` son parte del nombre) | Sí | No aplica |
| ¿Acceso? | Usuario/contraseña SQL | Credenciales + URLs firmadas | Permisos de red | HTTP/API |
| Ejemplo | PostgreSQL | Silo, MinIO, S3 | NAS, SMB | Spring Boot |

Por eso las fotos van al object storage y no a PostgreSQL: son archivos completos, no filas consultables.

---

## 5. ¿Qué es S3? AWS vs. el estándar

**S3 (Simple Storage Service)** es una **API HTTP para servidores de almacenamiento de objetos**, creada por Amazon en 2006. Define cómo pedir:

- "Guarda este archivo en este bucket con esta clave" (`PUT`)
- "Dámelo" (`GET`)
- "Bórralo" (`DELETE`)
- Cómo autenticarse (firmas)

No todos los object storage usan S3: Azure Blob y OpenStack Swift tienen APIs propias. Pero **S3 se volvió el estándar dominante**, y la mayoría de servidores lo implementan para ser compatibles.

La palabra "S3" se usa para dos cosas distintas:

| "S3" puede significar | Ejemplo |
|---|---|
| **El servicio en la nube de Amazon (AWS)** | `mi-bucket.s3.amazonaws.com` (se paga a Amazon) |
| **El protocolo/API estándar** | Cualquier servidor que hable el mismo idioma HTTP |

En este proyecto, "S3" casi siempre se refiere al **protocolo**.

```text
API S3 (el "idioma" estándar)
├── Amazon S3        → servicio original en la nube de AWS
├── MinIO            → servidor self-hosted que habla el idioma (archivado en 2026)
├── Silo             → fork de MinIO que habla el idioma (usado localmente)
├── Cloudflare R2    → proveedor que habla el idioma
└── LocalStack       → emulador local de servicios AWS
```

El backend usa el **AWS SDK v2**, el cliente oficial para hablar esa API. El mismo código funciona contra Silo local o contra Amazon S3 en producción: solo cambian el `endpoint` y las credenciales.

**Analogía:** S3 es como el estándar **USB-C**. Amazon lo popularizó, pero hoy muchos fabricantes hacen puertos compatibles. El cargador (SDK) funciona con cualquiera.

---

## 6. MinIO y Silo

- **MinIO**: servidor open source (Go) que convierte un disco o carpeta en un almacenamiento de objetos accesible por HTTP. Durante años fue el estándar para desarrollo local. Se levanta con Docker, se le da una carpeta (`/data`) y expone un endpoint (`:9000`) más una consola web (`:9001`).
- **Silo** (`pgsty/silo`): **fork comunitario de MinIO** creado tras el archivado del proyecto original en 2026. Usa el mismo CLI, las mismas variables de entorno y la misma API. Para el backend es indistinguible de MinIO.

**Contexto 2026:** MinIO archivó su repositorio y dejó de publicar las imágenes comunitarias en Docker Hub (septiembre de 2026); Quay también dejó de permitir el pull anónimo. Por eso el entorno local usa **Silo**, que sigue disponible y es compatible.

---

## 7. ¿Qué es una URL firmada?

El bucket es **privado**: abrir la ruta del objeto sin más devuelve **403**. Pero los clientes necesitan ver la foto. Opciones:

1. Hacer el bucket público → cualquiera ve todas las fotos. **Malo.**
2. Que el backend sirva cada imagen → funciona, pero carga al servidor y no escala.
3. **URL firmada (presigned URL)** → el backend genera un enlace temporal con una **firma criptográfica**. **Esto es lo que usa el proyecto.**

Ejemplo (firma abreviada):

```text
http://localhost:9000/reportes/incidentes/.../hd.jpg
  ?X-Amz-Algorithm=AWS4-HMAC-SHA256
  &X-Amz-Credential=.../20261025/us-east-1/s3/aws4_request
  &X-Amz-Date=20261025T030023Z
  &X-Amz-Expires=900
  &X-Amz-SignedHeaders=host
  &X-Amz-Signature=<hash>
```

- `X-Amz-Signature` es un hash (HMAC-SHA256) calculado con la **clave secreta** del servidor sobre los datos de la petición: método, ruta del objeto, fecha, expiración, etc.
- El servidor recalcula la firma; si coincide y no ha expirado, entrega el archivo. Si alguien cambia la ruta, alarga la expiración o inventa la URL, la firma no coincide → **403**.
- La clave secreta **nunca viaja al cliente**: solo la firma. Por eso no se puede falsificar.
- La URL está limitada a **ese objeto** y a **ese tiempo** (`X-Amz-Expires=900` = 15 minutos).

**Analogía:** es una **tarjeta de hotel** programada para una habitación concreta y una ventana de tiempo. No es la llave maestra; fuera de esa puerta o después del checkout, no sirve. También se parece a una entrada de concierto con fecha y sello antirréplica.

**Consecuencias prácticas:**

- La URL caduca a los 15 minutos → el dashboard debe pedir el reporte de nuevo para obtener una fresca. Por eso la BD guarda la **clave**, no la URL.
- Quien tenga la URL mientras esté vigente puede ver la foto (enlace "portador"). Es el compromiso habitual de este diseño.
- En el adaptador **local** no hay firma: el backend sirve el archivo directamente (solo desarrollo).

---

## 8. Cómo encaja todo en este proyecto

```text
App móvil / navegador / Postman  (CLIENTE)
        │  HTTP/JSON                    │  HTTP con URL firmada
        ▼                               ▼
Spring Boot :8080  (SERVIDOR de aplicaciones) ──► Silo :9000 (SERVIDOR de objetos, API S3)
        │
        ▼
PostgreSQL :5432  (SERVIDOR de base de datos)
```

- Spring Boot **es servidor** para el móvil y **es cliente** de PostgreSQL y Silo.
- PostgreSQL guarda los **datos** (reportes, usuarios, claves de imagen).
- Silo guarda **las fotos** y las entrega mediante URLs firmadas.
- Todo corre en el mismo PC en desarrollo; en producción cada componente puede estar en máquinas o contenedores distintos.

### Flujo de una foto

1. `POST /api/archivos` (multipart) → el backend sube el archivo a Silo con las credenciales.
2. Responde `{ clave, url_firmada }`.
3. El cliente abre la URL firmada → Silo valida firma y expiración → `200` con la foto.
4. `POST /api/reportes` con `claveImagenHd` → PostgreSQL guarda **solo la clave**.
5. `GET /api/reportes/{id}` → el backend genera una **URL firmada nueva** cada vez.

Las credenciales (`access key` / `secret key`) son como usuario y contraseña del servidor de almacenamiento. Se configuran en `.env` y en `application-dev.properties`, nunca en git.

---

## 9. Glosario y regla práctica

**Regla práctica:** cuando oigas "servidor", pregúntate **"¿servidor de qué?"**:

- "servidor **de base de datos**" → guarda filas y responde SQL (PostgreSQL).
- "servidor **de objetos**" → guarda archivos y responde S3 (Silo/MinIO/AWS S3).
- "servidor **de aplicaciones**" → ejecuta la lógica y responde HTTP (Spring Boot).
- "servidor **de archivos**" → comparte carpetas (SMB/NFS).
- "**el** servidor" a secas → normalmente "el programa que escucha en un puerto".

| Término | Significado |
|---|---|
| **Cliente** | Programa que hace peticiones |
| **Servidor** | Programa que escucha y responde |
| **Puerto** | "Puerta" lógica de un programa en una máquina (8080, 5432, 9000...) |
| **Endpoint** | URL base de una API (`http://localhost:9000`) |
| **Bucket** | Contenedor de objetos en un object storage |
| **Objeto / clave** | Archivo completo y su nombre único |
| **S3** | API HTTP estándar para almacenamiento de objetos |
| **MinIO / Silo** | Servidores locales que implementan la API S3 |
| **AWS SDK** | Cliente Java oficial para hablar la API S3 |
| **URL firmada** | Enlace temporal y criptográficamente validado a un objeto privado |
| **Contenedor** | Proceso aislado (Docker) con su propio sistema de archivos y red |

---

**En una frase:** un servidor es un programa que atiende peticiones en un puerto; un servidor de objetos guarda archivos completos; S3 es el idioma HTTP estándar que muchos de esos servidores hablan; y una URL firmada es un permiso temporal y verificable para ver un objeto privado concreto.
