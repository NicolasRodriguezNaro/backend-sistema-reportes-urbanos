package com.gestion.backend.geoespacial;

import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import com.gestion.backend.modelo.TipoIncidencia;

import jakarta.persistence.EntityManager;

@Service
public class ServicioGeoespacial {

	private static final int SRID_GPS = 4326;
	private static final int SRID_METROS = 9377;

	private final EntityManager entityManager;

	public ServicioGeoespacial(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Point a9377(double latitud, double longitud) {
		Object[] xy = (Object[]) entityManager.createNativeQuery(
				"SELECT ST_X(g), ST_Y(g) FROM (SELECT ST_Transform(ST_SetSRID(ST_MakePoint(:lon, :lat), "
						+ SRID_GPS + "), " + SRID_METROS + ") AS g) t")
				.setParameter("lon", longitud)
				.setParameter("lat", latitud)
				.getSingleResult();

		GeometryFactory factory = new GeometryFactory(new PrecisionModel(), SRID_METROS);
		return factory.createPoint(new Coordinate(((Number) xy[0]).doubleValue(), ((Number) xy[1]).doubleValue()));
	}

	public CoordenadasGps a4326(Point puntoEnMetros) {
		String ewkt = "SRID=" + SRID_METROS + ";POINT(" + puntoEnMetros.getX() + " " + puntoEnMetros.getY() + ")";

		Object[] grados = (Object[]) entityManager.createNativeQuery(
				"SELECT ST_Y(g), ST_X(g) FROM (SELECT ST_Transform(ST_GeomFromEWKT(:ewkt), "
						+ SRID_GPS + ") AS g) t")
				.setParameter("ewkt", ewkt)
				.getSingleResult();

		return new CoordenadasGps(((Number) grados[0]).doubleValue(), ((Number) grados[1]).doubleValue());
	}

	public Optional<Long> buscarReportePadre(Point puntoEnMetros, double radioMetros, TipoIncidencia categoria) {
		if (categoria == null) {
			return Optional.empty();
		}

		String ewkt = "SRID=" + SRID_METROS + ";POINT(" + puntoEnMetros.getX() + " " + puntoEnMetros.getY() + ")";

		List<?> filas = entityManager.createNativeQuery(
				"SELECT id, reporte_padre_id FROM reportes"
						+ " WHERE ST_DWithin(ubicacion, ST_GeomFromEWKT(:ewkt), :radio)"
						+ " AND categoria_ia = :categoria"
						+ " ORDER BY ubicacion <-> ST_GeomFromEWKT(:ewkt)"
						+ " LIMIT 1")
				.setParameter("ewkt", ewkt)
				.setParameter("radio", radioMetros)
				.setParameter("categoria", categoria.name())
				.getResultList();

		if (filas.isEmpty()) {
			return Optional.empty();
		}

		Object[] fila = (Object[]) filas.get(0);
		Number id = (Number) fila[0];
		Number padreId = (Number) fila[1];
		return Optional.of(padreId != null ? padreId.longValue() : id.longValue());
	}

	public record CoordenadasGps(double latitud, double longitud) {
	}
}
