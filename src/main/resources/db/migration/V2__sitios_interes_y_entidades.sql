-- ============================================================================
-- V2 - Modulo semantico: sitios de interes, entidades responsables y semillas
-- ============================================================================

-- Sitios de interes: infraestructura critica de la ciudad.
-- El tipo de geometria es generico porque el shapefile real puede traer
-- poligonos (hospitales, colegios) o lineas (vias principales).
CREATE TABLE sitios_interes (
    gid serial PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    tipo_sitio VARCHAR(50) NOT NULL,
    radio_influencia DOUBLE PRECISION NOT NULL,
    geom geometry(Geometry, 9377) NOT NULL
);

CREATE INDEX idx_sitios_interes_geom ON sitios_interes USING gist (geom);

-- Entidades responsables (ids canonicos 1-3 alineados con la ontologia).
INSERT INTO entidades (id, nombre, tipo, contacto_email) VALUES
    (1, 'Secretaria de Obras Publicas', 'SECRETARIA_OBRAS_PUBLICAS', 'obras@municipio.gov.co'),
    (2, 'Empresa de Acueducto y Alcantarillado', 'EMPRESA_SERVICIOS_PUBLICOS', 'acueducto@empresa.gov.co'),
    (3, 'Empresa de Aseo Urbano', 'EMPRESA_ASEO', 'aseo@empresa.gov.co')
ON CONFLICT (id) DO NOTHING;

SELECT setval('entidades_id_seq', GREATEST(3, (SELECT max(id) FROM entidades)), true);

-- Sitios DEMO para desarrollo (reemplazables por el shapefile real).
-- Coordenadas en WGS84 y transformadas a EPSG:9377 al insertar.
INSERT INTO sitios_interes (nombre, tipo_sitio, radio_influencia, geom) VALUES
    ('Hospital Departamental (demo)', 'HOSPITAL', 300,
        ST_Transform(ST_SetSRID(ST_MakePoint(-75.6070, 1.6150), 4326), 9377)),
    ('Colegio San Jose (demo)', 'COLEGIO', 150,
        ST_Transform(ST_SetSRID(ST_MakePoint(-75.6050, 1.6130), 4326), 9377));
