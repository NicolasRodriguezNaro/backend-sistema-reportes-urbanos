-- ============================================================================
-- V1 - Esquema inicial
-- Plataforma Inteligente de Gestion de Incidencias Urbanas
--
-- Nota: los DATOS de la tabla barrios NO van en esta migracion; se importan
-- aparte con shp2pgsql en modo append (-a) despues de aplicar V1.
-- ============================================================================

-- Extension geoespacial (idempotente para cualquier entorno)
CREATE EXTENSION IF NOT EXISTS postgis;

-- SRID 9377 (MAGNA-SIRGAS / Origen-Nacional, Colombia - metros).
-- Verificado: no viene incluido en PostGIS 3.5.3 de este equipo; se registra aqui.
INSERT INTO spatial_ref_sys (srid, auth_name, auth_srid, srtext, proj4text)
VALUES (
    9377,
    'EPSG',
    9377,
    'PROJCS["MAGNA-SIRGAS / Origen-Nacional",GEOGCS["MAGNA-SIRGAS",DATUM["Marco_Geocentrico_Nacional_de_Referencia",SPHEROID["GRS 1980",6378137,298.257222101]],PRIMEM["Greenwich",0],UNIT["degree",0.0174532925199433]],PROJECTION["Transverse_Mercator"],PARAMETER["latitude_of_origin",4.0],PARAMETER["central_meridian",-73.0],PARAMETER["scale_factor",0.9992],PARAMETER["false_easting",5000000.0],PARAMETER["false_northing",2000000.0],UNIT["metre",1]]',
    '+proj=tmerc +lat_0=4 +lon_0=-73 +k=0.9992 +x_0=5000000 +y_0=2000000 +ellps=GRS80 +units=m +no_defs'
)
ON CONFLICT (srid) DO NOTHING;

-- Tabla barrios: estructura (los datos llegan por shp2pgsql -a)
CREATE TABLE barrios (
    gid serial NOT NULL PRIMARY KEY,
    fid numeric,
    objectid numeric,
    nombre varchar(100),
    shape_leng numeric,
    shape_area numeric,
    cod_comuna numeric,
    cod_barrio numeric,
    geom geometry(MultiPolygon, 9377)
);

CREATE INDEX barrios_geom_idx ON barrios USING gist (geom);

-- Tabla entidades
CREATE TABLE entidades (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    contacto_email VARCHAR(150) NOT NULL UNIQUE
);

-- Tabla usuarios
-- (sin indice extra en email: la restriccion UNIQUE ya crea uno)
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL DEFAULT 'CIUDADANO',
    entidad_id INT,
    fecha_registro TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Relacion N:1 con entidades (opcional, para usuarios funcionarios)
    CONSTRAINT fk_usuarios_entidades
        FOREIGN KEY (entidad_id)
        REFERENCES entidades(id)
        ON DELETE SET NULL
);

-- Tabla reportes
CREATE TABLE reportes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    barrio_id INT, -- Referencia a la clave primaria (gid) de la tabla barrios
    entidad_asignada_id INT,
    reporte_padre_id BIGINT,

    -- Ubicacion geoespacial en metros (EPSG:9377 - MAGNA-SIRGAS Origen Nacional)
    ubicacion GEOMETRY(Point, 9377) NOT NULL,

    -- Resultados de clasificacion por Inteligencia Artificial
    categoria_ia VARCHAR(100),
    severidad_ia VARCHAR(50),
    confianza_ia DOUBLE PRECISION,
    descripcion TEXT,

    -- URLs de imagenes guardadas en Object Storage (MinIO / S3)
    url_imagen_hd VARCHAR(500) NOT NULL,
    url_imagen_thumb VARCHAR(500),

    -- Estado y prioridad del reporte
    estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
    prioridad_calculada VARCHAR(50),

    -- Trazabilidad temporal
    fecha_creacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Restricciones de integridad
    CONSTRAINT fk_reportes_usuarios
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_reportes_barrios
        FOREIGN KEY (barrio_id)
        REFERENCES barrios(gid)
        ON DELETE SET NULL,
    CONSTRAINT fk_reportes_entidades
        FOREIGN KEY (entidad_asignada_id)
        REFERENCES entidades(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_reportes_padre
        FOREIGN KEY (reporte_padre_id)
        REFERENCES reportes(id)
        ON DELETE SET NULL
);

-- Indice espacial GiST para consultas de cercania (deduplicacion ST_DWithin)
CREATE INDEX idx_reportes_ubicacion ON reportes USING GIST(ubicacion);

-- Indices B-Tree para filtros frecuentes del dashboard
CREATE INDEX idx_reportes_usuario_id ON reportes(usuario_id);
CREATE INDEX idx_reportes_barrio_id ON reportes(barrio_id);
CREATE INDEX idx_reportes_entidad_asignada ON reportes(entidad_asignada_id);
CREATE INDEX idx_reportes_estado ON reportes(estado);
CREATE INDEX idx_reportes_reporte_padre ON reportes(reporte_padre_id);
