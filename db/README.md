# Importacion de capas geoespaciales

Las capas viven en PostgreSQL + PostGIS (SRID 9377, MAGNA-SIRGAS Origen Nacional).
Los datos son independientes del esquema: el esquema lo gobierna Flyway (V1, V2...).

## Requisitos

- PostgreSQL 17 con PostGIS (incluye `shp2pgsql` en `C:\Program Files\PostgreSQL\17\bin`).
- `psql` con la clave del usuario `postgres` (`$env:PGPASSWORD` en PowerShell o `PGPASSWORD` en bash).

## Barrios (ya importados: 180 poligonos en 9377)

La tabla `barrios` tiene FK desde `reportes`, por lo que NO se puede re-crear con
`shp2pgsql` en modo normal. Para actualizarla:

```bash
# 1) Vaciar y reimportar como append (mantiene estructura, PK, indice y FK)
psql -U postgres -h localhost -d gestion_db -c "TRUNCATE barrios;"
shp2pgsql -a -s 9377 "C:\ruta\barrios.shp" public.barrios | psql -U postgres -h localhost -d gestion_db
```

## Sitios de interes (SitiosInteres.shp, CRS EPSG:3116)

El shapefile de Florencia viene en puntos y EPSG:3116 (MAGNA-SIRGAS / Colombia Bogota).
La tabla destino usa codigos de tipo y radio de influencia, por eso se importa a una
tabla de staging y se transforma con `INSERT ... SELECT`.

```bash
# 1) Staging con reproyeccion 3116 -> 9377
shp2pgsql -c -D -s 3116:9377 "C:\ruta\SitiosInteres.shp" public.sitios_import | psql -U postgres -h localhost -d gestion_db

# 2) Mapeo de tipos del shapefile a codigos del dominio + radios por tipo
psql -U postgres -h localhost -d gestion_db <<'SQL'
INSERT INTO sitios_interes (nombre, tipo_sitio, radio_influencia, geom)
SELECT nombre,
  CASE
    WHEN tipo ILIKE 'Hospital%' THEN 'HOSPITAL'
    WHEN tipo ILIKE 'Instituci%n educativa%' THEN 'INSTITUCION_EDUCATIVA'
    WHEN tipo ILIKE 'Universidad%' THEN 'UNIVERSIDAD'
    WHEN tipo ILIKE 'Transporte%' THEN 'TRANSPORTE'
    WHEN tipo ILIKE 'Entidad gubernamental%' THEN 'ENTIDAD_GUBERNAMENTAL'
    WHEN tipo ILIKE 'Seguridad%' THEN 'SEGURIDAD'
    WHEN tipo ILIKE 'Militar%' THEN 'MILITAR'
    WHEN tipo ILIKE 'C%rcel%' THEN 'CARCEL'
    WHEN tipo ILIKE 'Centro comercial%' THEN 'CENTRO_COMERCIAL'
    WHEN tipo ILIKE 'Banco%' THEN 'BANCO'
    WHEN tipo ILIKE 'Servicios p%blicos%' THEN 'SERVICIOS_PUBLICOS'
    WHEN tipo ILIKE 'Servicios sociales%' THEN 'SERVICIOS_SOCIALES'
    WHEN tipo ILIKE 'Protecci%n social%' THEN 'PROTECCION_SOCIAL'
    WHEN tipo ILIKE 'Deportivo%' THEN 'DEPORTIVO'
    WHEN tipo ILIKE 'Recreativo%' THEN 'RECREATIVO'
    WHEN tipo ILIKE 'Catedral%' THEN 'CATEDRAL'
    WHEN tipo ILIKE 'Justicia%' THEN 'JUSTICIA'
    ELSE 'OTRO'
  END,
  CASE
    WHEN tipo ILIKE 'Hospital%' THEN 300
    WHEN tipo ILIKE 'Instituci%n educativa%' THEN 150
    WHEN tipo ILIKE 'Universidad%' THEN 200
    ELSE 100
  END,
  geom
FROM sitios_import;

DROP TABLE sitios_import;
SQL
```

Solo los tipos criticos (`HOSPITAL`, `INSTITUCION_EDUCATIVA`, `UNIVERSIDAD`, `VIA_ARTERIA`)
participan del razonamiento semantico; el resto se conserva para el dashboard/mapa.

## Sincronizacion del grafo semantico

El espejo de `entidades` y `sitios_interes` se reconstruye **al arrancar la aplicacion**
(`ApplicationReadyEvent`). Despues de importar o modificar capas, reinicia el backend.

- Grafo persistente TDB2: `data/jena` (ignorado por git).
- Tests: `target/jena-test-*` (no ejecutar tests y app a la vez: TDB2 bloquea la carpeta).

## Verificacion rapida

```sql
SELECT count(*) AS sitios,
       count(*) FILTER (WHERE tipo_sitio IN ('HOSPITAL','INSTITUCION_EDUCATIVA','UNIVERSIDAD','VIA_ARTERIA')) AS criticos
FROM sitios_interes;

SELECT f_table_name, type, srid FROM geometry_columns ORDER BY f_table_name;
```
