# Módulo semántico — Documentación técnica

**Proyecto:** Plataforma Inteligente de Gestión de Incidencias Urbanas
**Backend:** Spring Boot 4.1.1 · Java 17 · PostgreSQL 17 + PostGIS 3.5.3 · Apache Jena 5.6.0 (TDB2)
**Ontología:** `ontologia.ttl` v1.1.0
**Última actualización:** octubre de 2026

---

## 1. Propósito

Cuando llega un reporte, la plataforma ya sabe **qué es** (módulo IA) y **dónde está** (PostGIS). Faltan las dos decisiones de negocio:

1. **¿Qué tan urgente es?** → si está cerca de una zona crítica, se eleva la prioridad.
2. **¿Quién debe atenderlo?** → según el tipo de incidencia, se asigna la entidad competente.

El módulo semántico responde esas preguntas con **conocimiento explícito y reglas de inferencia**, en lugar de condicionales dispersos en el código. La ontología, las competencias y las reglas viven versionadas en el repositorio, y las decisiones son consultables y explicables.

---

## 2. Arquitectura: responsabilidades

| Componente | Responsabilidad | Dato que posee |
|---|---|---|
| **PostgreSQL** | Verdad operativa: reportes, estados, prioridad y entidad asignada | Filas de negocio |
| **PostGIS** | Cálculo espacial: deduplicación a 5 m, distancia a sitios, barrio contenedor | Geometrías (SRID 9377) |
| **Apache Jena (TDB2)** | Conocimiento: ontología, individuos, competencias, inferencias | Tripletas RDF |
| **Reglas Jena** | Política de priorización y asignación | Reglas declarativas |
| **Java** | Orquestación y resolución de conflictos entre candidatos | — |

Principio de diseño: **PostGIS calcula distancias; el grafo razona sobre relaciones**. El grafo no guarda coordenadas (no duplica geometrías) y la base de datos no guarda reglas (no duplica conocimiento).

---

## 3. Ontología (`src/main/resources/semantica/ontologia.ttl`, v1.1.0)

### 3.1 Jerarquía de conceptos

```text
IncidenciaUrbana
├── DeterioroVial
│   ├── HuecoVia            ← enum HUECO_VIA
│   └── FallaSemaforica     (categoría futura)
├── FallaServicioPublico
│   ├── FugaAgua            ← enum FUGA_AGUA
│   └── FallaAlumbrado      ← enum FALLA_ALUMBRADO
└── ResiduoUrbano
    ├── AcumulacionBasura   ← enum ACUMULACION_BASURA
    └── Escombros           (categoría futura)
AtencionCritica             ← clase inferida por regla

SitioInteres
└── ZonaCritica
    ├── CentroSalud ── Hospital
    ├── InstitucionEducativa ── Colegio, Universidad
    └── ViaPrincipal ── Avenida

EntidadResponsable
├── SecretariaObrasPublicas
├── EmpresaServiciosPublicos
└── EmpresaAseo
```

Las tres ramas raíz son disjuntas (`owl:disjointWith`): un nodo no puede ser a la vez incidencia, sitio y entidad.

### 3.2 Propiedades

| Tipo | Propiedad | Dominio → Rango |
|---|---|---|
| Objeto | `cercanoA` | IncidenciaUrbana → SitioInteres |
| Objeto | `esCompetentePara` | EntidadResponsable → IncidenciaUrbana |
| Objeto | `asignadoA` (inferida) | IncidenciaUrbana → EntidadResponsable |
| Datos | `reporteId`, `severidadIA`, `confianzaIA`, `prioridadCalculada`, `distanciaMetros` | IncidenciaUrbana |
| Datos | `sitioId` | SitioInteres |
| Datos | `entidadId` | EntidadResponsable (puente grafo → PostgreSQL) |

### 3.3 Política de competencias (declarada en el TTL, no en Java)

```turtle
ex:Entidad_1 ex:esCompetentePara ex:HuecoVia .
ex:Entidad_2 ex:esCompetentePara ex:FugaAgua, ex:FallaAlumbrado .
ex:Entidad_3 ex:esCompetentePara ex:AcumulacionBasura .
```

Si cambia una competencia, se edita este archivo y se versiona; no se recompila código.

---

## 4. Reglas de inferencia (`src/main/resources/semantica/reglas.rules`)

Son 10 reglas forward-chaining:

| Regla | Qué infiere |
|---|---|
| `clausuraSubclase` | Cierra transitivamente la jerarquía (`HuecoVia ⊑ DeterioroVial ⊑ IncidenciaUrbana`) |
| `asignarEntidad` | Si la incidencia es de un tipo con entidad competente → `asignadoA` |
| `marcarAtencionCritica` | Si está `cercanoA` a un sitio cuyo tipo cuelga de `ZonaCritica` → la incidencia es `AtencionCritica` |
| `prioridadBaseBaja/Media/Alta/Critica` | Candidato de prioridad según `severidadIA` |
| `prioridadZonaAlta` | Zona crítica → candidato mínimo `ALTA` |
| `prioridadZonaCriticaSeveridadAlta/Critica` | Zona crítica + severidad ALTA/CRÍTICA → candidato `CRITICA` |

**Resolución de conflictos (Java):** las reglas producen *candidatos*; `ServicioSemantico` elige el más fuerte (`CRITICA > ALTA > MEDIA > BAJA`). El motor declara conocimiento; la política de negocio resuelve.

---

## 5. Almacenamiento: Apache Jena TDB2

- Ruta de datos: `data/jena` (ignorada por git). Persistente entre reinicios.
- Tres grafos:
  - `urn:gestion:ontologia` → el TTL, recargado en cada arranque (el repositorio manda).
  - `urn:gestion:referencia` → espejo de `entidades` y `sitios_interes`, reconstruido en cada arranque.
  - grafo por defecto → instancias dinámicas de reportes (`ex:Reporte_{id}`).
- La inferencia se ejecuta sobre una copia en memoria de la unión de grafos; solo se **materializan** las conclusiones finales (prioridad elegida y entidad asignada).
- El grafo es **derivado y reconstruible**: si se elimina, se regenera desde la ontología y PostgreSQL.
- Se cierra con `@PreDestroy`. TDB2 bloquea su carpeta: no ejecutar la app y los tests al mismo tiempo.

---

## 6. Sincronización del espejo estático

Al evento `ApplicationReadyEvent` (después de Flyway y con la BD lista):

1. Lee `entidades` y `sitios_interes` completos.
2. Genera IRIs canónicas desde el id: `ex:Entidad_{id}` y `ex:Sitio_{gid}`.
3. `rdfs:label` almacena el nombre legible (cambiable sin romper identidades).
4. Mapea el tipo a clase ontológica:
   - `HOSPITAL` → `Hospital`
   - `INSTITUCION_EDUCATIVA` → `Colegio`
   - `UNIVERSIDAD` → `Universidad`
   - `VIA_ARTERIA` → `ViaPrincipal`
   - demás tipos → `SitioInteres` genérico (no crítico)

**Datos actuales:** 3 entidades (ids 1–3), **36 sitios reales** de Florencia (14 críticos: 2 hospitales, 10 instituciones educativas, 2 universidades), radios de influencia 300/150/200/100 m.

---

## 7. Flujo completo de un reporte

```text
POST /api/reportes
   │
   ├─ 1. IA completa campos vacíos (mock en dev): categoría, severidad, confianza, borrador
   ├─ 2. PostGIS transforma GPS 4326 → 9377
   ├─ 3. Deduplicación: ¿otro reporte de la MISMA categoría a ≤ 5 m? → padre / nuevo
   ├─ 4. Barrio: ST_Intersects(barrios.geom, punto) → barrio_id
   ├─ 5. INSERT en PostgreSQL (se necesita el id para el RDF)
   ├─ 6. Módulo semántico:
   │      a. PostGIS: sitio CRÍTICO más cercano dentro de su radio (LIMIT 1)
   │      b. Grafo: escribe ex:Reporte_{id} + tipo + severidad + cercanoA + distancia
   │      c. Jena: aplica las 10 reglas
   │      d. Java: resuelve candidatos de prioridad (el más fuerte gana)
   │      e. Grafo: materializa prioridadCalculada y asignadoA
   ├─ 7. UPDATE en PostgreSQL: prioridad_calculada y entidad_asignada_id
   └─ 8. Respuesta JSON (incluye prioridadCalculada, entidadAsignadaId, barrioId)
```

**Ejemplo real (reporte 78):** FUGA_AGUA, severidad ALTA, a 33.39 m del Hospital Departamental (demo en ese momento) → `AtencionCritica` → `prioridadCalculada = CRITICA`, `entidadAsignadaId = 2`.

**Tolerancia a fallos:** si el análisis semántico falla, la API no se cae: la prioridad cae al mapeo por severidad, la entidad queda nula y se registra un warning. La creación del reporte no depende del grafo.

---

## 8. Archivos del módulo

| Archivo | Rol |
|---|---|
| `src/main/resources/semantica/ontologia.ttl` | Ontología v1.1.0 (conocimiento estable + política de competencias) |
| `src/main/resources/semantica/reglas.rules` | Las 10 reglas de inferencia |
| `src/main/resources/db/migration/V2__sitios_interes_y_entidades.sql` | Tabla `sitios_interes`, entidades seed 1–3 y sitios demo (eliminados tras importar la capa real) |
| `semantica/GrafoSemantico.java` | Gateway TDB2: ontología, espejo, escritura de reportes, inferencia, materialización |
| `semantica/ServicioSemantico.java` | Orquesta PostGIS + grafo + resolución; sincroniza al arrancar |
| `semantica/AnalisisSemantico.java` | Resultado: prioridad + entidadId |
| `semantica/SitioCercano.java` | Sitio crítico más cercano (gid, nombre, tipo, distancia) |
| `semantica/InferenciasGrafo.java` | Candidatos de prioridad + entidad inferida |
| `servicio/ReporteService.java` | Integra IA, dedup, barrio, semántica y respuesta |
| `geoespacial/ServicioGeoespacial.java` | Transformaciones 4326↔9377, dedup, barrio (`ST_Intersects`) |
| `dto/ReporteResponse.java` | Expone `prioridadCalculada` |
| `db/README.md` | Procedimiento de importación de capas y re-sincronización |

---

## 9. Decisiones de diseño y problemas resueltos

1. **Fusión de ontologías:** se unificaron dos versiones divergentes (jerarquía de la generada en Protégé + operatividad de la manual) con nombres canónicos.
2. **PostGIS calcula, Jena razona:** evita GeoSPARQL y aprovecha los índices GiST.
3. **Un solo sitio crítico más cercano:** elimina la ambigüedad de `distanciaMetros`; la consulta usa `LIMIT 1` y el radio propio de cada sitio.
4. **Solo tipos críticos en la cercanía:** sin este filtro, un banco a 20 m eclipsaría un hospital a 150 m.
5. **Códigos string en lugar de enum:** la capa real trae 17 tipos; un enum de 3 era insostenible. Los críticos se mapean a clases ontológicas; el resto a `SitioInteres`.
6. **IRIs desde el id:** no se controlan nombres manualmente; el nombre es solo `rdfs:label`.
7. **Doble escritura no transaccional (PostgreSQL ↔ TDB2):** asumida y mitigada: el grafo es derivado, el análisis es idempotente (se borran y reinsertan los triples del reporte) y es reconstruible.
8. **Aislamiento de tests:** TDB separado por contexto (`target/jena-test-*`) para evitar bloqueos de TDB2.

---

## 10. Verificación y demostración

- **15/15 tests** en verde: 8 de API, 3 de semántica, 2 de barrio, 1 de geometría, 1 de contexto.

Prueba manual contra el Hospital María Inmaculada (lat `1.619537`, lon `-75.611134`):

```bash
curl -X POST http://localhost:8080/api/reportes \
  -H "Content-Type: application/json" \
  -d "{\"descripcion\":\"Fuga frente al hospital\",\"categoriaIa\":\"FUGA_AGUA\",\"severidadIa\":\"ALTA\",\"latitud\":1.619537,\"longitud\":-75.611134,\"urlImagenHd\":\"http://local/f5.jpg\",\"usuarioId\":5}"
```

Se espera: `prioridadCalculada = "CRITICA"`, `entidadAsignadaId = 2` y `barrioId` asignado si el punto cae dentro de la capa de barrios.

Evidencia espacial:

```sql
SELECT r.id, s.nombre, round(ST_Distance(r.ubicacion, s.geom)::numeric, 2) AS metros
FROM reportes r
JOIN sitios_interes s ON ST_DWithin(r.ubicacion, s.geom, s.radio_influencia)
WHERE r.id = 78;
```

---

## 11. Limitaciones y trabajo futuro

- **Re-análisis:** los reportes creados antes del módulo tienen prioridad/entidad nulas. Un `PATCH /api/reportes/{id}/reanalizar` lo resolvería (idempotente).
- **Reproducibilidad de datos:** la migración V2 siembra 2 sitios demo; al reconstruir una BD desde cero hay que eliminarlos después de importar la capa real (ver `db/README.md`).
- **Multi-sitio/ponderación:** si las reglas necesitan pesos por tipo, se requeriría modelado n-ario (reificación) o “más cercano por tipo”.
- **Competencias editables** desde el dashboard (tabla + sincronización) en lugar del TTL.
- **SHACL** para validación de calidad del grafo y **Fuseki** si el grafo se separa en producción.
- **Proveedor IA real:** hoy solo existe el clasificador simulado `@Profile("dev")`.

---

## 12. Resumen para el informe

> El módulo semántico de la plataforma implementa una ontología OWL ligera (v1.1.0) sobre Apache Jena/TDB2 que modela incidencias urbanas, zonas críticas y entidades responsables, junto con reglas de producción para priorización y asignación. Al crear un reporte, PostGIS determina el sitio crítico más cercano dentro de su radio de influencia; el backend registra la instancia RDF del reporte, ejecuta las reglas de inferencia (competencia de entidades, clasificación como atención crítica y elevación de prioridad) y materializa la decisión. El resultado se persiste en PostgreSQL (`prioridad_calculada`, `entidad_asignada_id`), manteniendo el grafo como capa de conocimiento reconstruible y explicable, y a PostgreSQL como fuente de verdad operativa. La arquitectura desacopla el razonamiento del código de negocio: las políticas de la ciudad se expresan como triples y reglas versionadas.
