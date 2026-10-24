package com.gestion.backend.semantica;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.gestion.backend.modelo.PrioridadCalculada;
import com.gestion.backend.modelo.Reporte;
import com.gestion.backend.modelo.Severidad;
import com.gestion.backend.modelo.TipoEntidad;

import jakarta.persistence.EntityManager;

@Service
public class ServicioSemantico {

	private static final Logger log = LoggerFactory.getLogger(ServicioSemantico.class);

	private final EntityManager entityManager;
	private final GrafoSemantico grafo;

	public ServicioSemantico(EntityManager entityManager, GrafoSemantico grafo) {
		this.entityManager = entityManager;
		this.grafo = grafo;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void sincronizarAlArrancar() {
		try {
			sincronizarReferencia();
			log.info("Grafo semantico sincronizado con las tablas de referencia");
		} catch (Exception e) {
			log.warn("No se pudo sincronizar el grafo semantico al arrancar", e);
		}
	}

	public void sincronizarReferencia() {
		List<GrafoSemantico.EntidadRef> entidades = new ArrayList<>();
		List<?> filasEntidades = entityManager
				.createNativeQuery("SELECT id, nombre, tipo FROM entidades ORDER BY id")
				.getResultList();
		for (Object fila : filasEntidades) {
			Object[] datos = (Object[]) fila;
			TipoEntidad tipo = parsearTipoEntidad((String) datos[2]);
			if (tipo != null) {
				entidades.add(new GrafoSemantico.EntidadRef(((Number) datos[0]).intValue(), (String) datos[1], tipo));
			}
		}

		List<GrafoSemantico.SitioRef> sitios = new ArrayList<>();
		List<?> filasSitios = entityManager
				.createNativeQuery("SELECT gid, nombre, tipo_sitio FROM sitios_interes ORDER BY gid")
				.getResultList();
		for (Object fila : filasSitios) {
			Object[] datos = (Object[]) fila;
			sitios.add(new GrafoSemantico.SitioRef(((Number) datos[0]).intValue(), (String) datos[1],
					((String) datos[2]).trim().toUpperCase()));
		}

		grafo.sincronizarEstaticos(entidades, sitios);
	}

	public AnalisisSemantico analizar(Reporte reporte) {
		try {
			Optional<SitioCercano> sitio = buscarSitioCercano(reporte.getUbicacion());
			grafo.escribirReporte(reporte.getId(), reporte.getCategoriaIa(), reporte.getSeveridadIa(),
					reporte.getConfianzaIa(), sitio.orElse(null));

			InferenciasGrafo inferencias = grafo.inferir(reporte.getId());
			PrioridadCalculada prioridad = resolverPrioridad(inferencias.prioridades(), reporte.getSeveridadIa());
			Integer entidadId = inferencias.entidadId().orElse(null);

			grafo.materializar(reporte.getId(), prioridad, entidadId);

			return new AnalisisSemantico(prioridad, entidadId);
		} catch (Exception e) {
			log.warn("El analisis semantico fallo para el reporte {}; se usa prioridad por severidad",
					reporte.getId(), e);
			return new AnalisisSemantico(prioridadPorSeveridad(reporte.getSeveridadIa()), null);
		}
	}

	private Optional<SitioCercano> buscarSitioCercano(Point ubicacion) {
		String ewkt = "SRID=9377;POINT(" + ubicacion.getX() + " " + ubicacion.getY() + ")";

		List<?> filas = entityManager.createNativeQuery(
				"SELECT s.gid, s.nombre, s.tipo_sitio,"
						+ " ST_Distance(s.geom, ST_GeomFromEWKT(:ewkt)) AS distancia"
						+ " FROM sitios_interes s"
						+ " WHERE s.tipo_sitio IN ('HOSPITAL', 'INSTITUCION_EDUCATIVA', 'UNIVERSIDAD', 'VIA_ARTERIA')"
						+ " AND ST_DWithin(s.geom, ST_GeomFromEWKT(:ewkt), s.radio_influencia)"
						+ " ORDER BY distancia"
						+ " LIMIT 1")
				.setParameter("ewkt", ewkt)
				.getResultList();

		if (filas.isEmpty()) {
			return Optional.empty();
		}

		Object[] fila = (Object[]) filas.get(0);
		String tipoSitio = ((String) fila[2]).trim().toUpperCase();

		return Optional.of(new SitioCercano(((Number) fila[0]).intValue(), (String) fila[1], tipoSitio,
				((Number) fila[3]).doubleValue()));
	}

	private PrioridadCalculada resolverPrioridad(Set<String> candidatos, Severidad severidad) {
		PrioridadCalculada elegida = null;
		for (String candidato : candidatos) {
			try {
				PrioridadCalculada prioridad = PrioridadCalculada.valueOf(candidato);
				if (elegida == null || prioridad.ordinal() > elegida.ordinal()) {
					elegida = prioridad;
				}
			} catch (IllegalArgumentException e) {
				log.warn("Candidato de prioridad desconocido: {}", candidato);
			}
		}
		return elegida != null ? elegida : prioridadPorSeveridad(severidad);
	}

	private PrioridadCalculada prioridadPorSeveridad(Severidad severidad) {
		if (severidad == null) {
			return null;
		}
		return switch (severidad) {
			case BAJA -> PrioridadCalculada.BAJA;
			case MEDIA -> PrioridadCalculada.MEDIA;
			case ALTA -> PrioridadCalculada.ALTA;
			case CRITICA -> PrioridadCalculada.CRITICA;
		};
	}

	private TipoEntidad parsearTipoEntidad(String valor) {
		try {
			return TipoEntidad.valueOf(valor.trim().toUpperCase());
		} catch (Exception e) {
			log.warn("Tipo de entidad desconocido en la BD: {}", valor);
			return null;
		}
	}
}
