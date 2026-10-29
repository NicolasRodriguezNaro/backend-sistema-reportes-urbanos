package com.gestion.backend.servicio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.backend.almacenamiento.ResolvedorUrlArchivos;
import com.gestion.backend.dto.ConteoBarrio;
import com.gestion.backend.dto.DashboardResumenResponse;
import com.gestion.backend.dto.FeatureCollection;
import com.gestion.backend.modelo.EstadoReporte;
import com.gestion.backend.modelo.PrioridadCalculada;
import com.gestion.backend.modelo.TipoIncidencia;
import com.gestion.backend.modelo.Usuario;
import com.gestion.backend.seguridad.AlcanceReportes;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Service
public class DashboardService {

	private final EntityManager entityManager;
	private final AlcanceReportes alcanceReportes;
	private final ResolvedorUrlArchivos resolvedorUrlArchivos;

	public DashboardService(EntityManager entityManager, AlcanceReportes alcanceReportes,
			ResolvedorUrlArchivos resolvedorUrlArchivos) {
		this.entityManager = entityManager;
		this.alcanceReportes = alcanceReportes;
		this.resolvedorUrlArchivos = resolvedorUrlArchivos;
	}

	@Transactional(readOnly = true)
	public DashboardResumenResponse resumen(LocalDate desde, LocalDate hasta, Integer barrioId,
			Integer entidadId) {
		Usuario usuario = alcanceReportes.usuarioActual();
		Integer entidadFiltro = alcanceReportes.esAdministrador(usuario)
				? entidadId
				: alcanceReportes.entidadIdObligatoria(usuario);

		FiltroSql filtro = filtroSql(entidadFiltro, null, null, null, barrioId,
				inicio(desde), finExclusivo(hasta), null);

		long total = contar("SELECT count(*) FROM reportes" + filtro.where(), filtro.parametros());
		Map<EstadoReporte, Long> porEstado = contarPor("estado", EstadoReporte.class, filtro);
		Map<TipoIncidencia, Long> porCategoria = contarPor("categoria_ia", TipoIncidencia.class, filtro);
		Map<PrioridadCalculada, Long> porPrioridad = contarPor("prioridad_calculada", PrioridadCalculada.class,
				filtro);

		long criticos = contar("SELECT count(*) FROM reportes" + filtro.where()
				+ " AND prioridad_calculada = 'CRITICA'", filtro.parametros());
		long sinAsignar = contar("SELECT count(*) FROM reportes" + filtro.where()
				+ " AND entidad_asignada_id IS NULL", filtro.parametros());

		return new DashboardResumenResponse(total, porEstado, porCategoria, porPrioridad, criticos, sinAsignar,
				porBarrio(filtro));
	}

	@Transactional(readOnly = true)
	public FeatureCollection geojsonReportes(LocalDate desde, LocalDate hasta, EstadoReporte estado,
			TipoIncidencia categoria, PrioridadCalculada prioridad, Integer barrioId, Integer entidadId,
			String texto) {
		Usuario usuario = alcanceReportes.usuarioActual();
		Integer entidadFiltro = alcanceReportes.esAdministrador(usuario)
				? entidadId
				: alcanceReportes.entidadIdObligatoria(usuario);

		FiltroSql filtro = filtroSql(entidadFiltro, estado, categoria, prioridad, barrioId,
				inicio(desde), finExclusivo(hasta), texto);

		Query consulta = entityManager.createNativeQuery(
				"SELECT id, estado, categoria_ia, prioridad_calculada, url_imagen_thumb, fecha_creacion,"
						+ " ST_X(ST_Transform(ubicacion, 4326)), ST_Y(ST_Transform(ubicacion, 4326))"
						+ " FROM reportes" + filtro.where()
						+ " ORDER BY fecha_creacion DESC LIMIT 2000");
		filtro.parametros().forEach(consulta::setParameter);

		List<FeatureCollection.Feature> features = new ArrayList<>();
		for (Object fila : consulta.getResultList()) {
			Object[] datos = (Object[]) fila;
			Map<String, Object> propiedades = new LinkedHashMap<>();
			propiedades.put("id", ((Number) datos[0]).longValue());
			propiedades.put("estado", datos[1]);
			propiedades.put("categoria", datos[2]);
			propiedades.put("prioridad", datos[3]);
			propiedades.put("thumbUrl", resolvedorUrlArchivos.resolver((String) datos[4]));
			propiedades.put("fechaCreacion", fechaIso(datos[5]));
			features.add(FeatureCollection.Feature.punto(
					((Number) datos[6]).doubleValue(), ((Number) datos[7]).doubleValue(), propiedades));
		}
		return FeatureCollection.de(features);
	}

	private long contar(String sql, Map<String, Object> parametros) {
		Query consulta = entityManager.createNativeQuery(sql);
		parametros.forEach(consulta::setParameter);
		return ((Number) consulta.getSingleResult()).longValue();
	}

	private <E extends Enum<E>> Map<E, Long> contarPor(String columna, Class<E> tipo, FiltroSql filtro) {
		Map<E, Long> resultado = new LinkedHashMap<>();
		for (E valor : tipo.getEnumConstants()) {
			resultado.put(valor, 0L);
		}

		Query consulta = entityManager.createNativeQuery(
				"SELECT " + columna + ", count(*) FROM reportes" + filtro.where()
						+ " AND " + columna + " IS NOT NULL GROUP BY " + columna);
		filtro.parametros().forEach(consulta::setParameter);

		for (Object fila : consulta.getResultList()) {
			Object[] datos = (Object[]) fila;
			resultado.put(Enum.valueOf(tipo, (String) datos[0]), ((Number) datos[1]).longValue());
		}
		return resultado;
	}

	private List<ConteoBarrio> porBarrio(FiltroSql filtro) {
		Query consulta = entityManager.createNativeQuery(
				"SELECT barrio_id, (SELECT nombre FROM barrios WHERE gid = reportes.barrio_id), count(*)"
						+ " FROM reportes" + filtro.where() + " AND barrio_id IS NOT NULL"
						+ " GROUP BY barrio_id ORDER BY count(*) DESC LIMIT 10");
		filtro.parametros().forEach(consulta::setParameter);

		List<ConteoBarrio> resultado = new ArrayList<>();
		for (Object fila : consulta.getResultList()) {
			Object[] datos = (Object[]) fila;
			resultado.add(new ConteoBarrio(((Number) datos[0]).intValue(),
					(String) datos[1], ((Number) datos[2]).longValue()));
		}
		return resultado;
	}

	private FiltroSql filtroSql(Integer entidadId, EstadoReporte estado, TipoIncidencia categoria,
			PrioridadCalculada prioridad, Integer barrioId, Instant desde, Instant hasta, String texto) {
		StringBuilder where = new StringBuilder(" WHERE 1=1");
		Map<String, Object> parametros = new LinkedHashMap<>();

		if (entidadId != null) {
			where.append(" AND entidad_asignada_id = :entidadId");
			parametros.put("entidadId", entidadId);
		}
		if (estado != null) {
			where.append(" AND estado = :estado");
			parametros.put("estado", estado.name());
		}
		if (categoria != null) {
			where.append(" AND categoria_ia = :categoria");
			parametros.put("categoria", categoria.name());
		}
		if (prioridad != null) {
			where.append(" AND prioridad_calculada = :prioridad");
			parametros.put("prioridad", prioridad.name());
		}
		if (barrioId != null) {
			where.append(" AND barrio_id = :barrioId");
			parametros.put("barrioId", barrioId);
		}
		if (desde != null) {
			where.append(" AND fecha_creacion >= :desde");
			parametros.put("desde", desde);
		}
		if (hasta != null) {
			where.append(" AND fecha_creacion < :hasta");
			parametros.put("hasta", hasta);
		}
		if (texto != null && !texto.isBlank()) {
			where.append(" AND lower(descripcion) LIKE :texto");
			parametros.put("texto", "%" + texto.trim().toLowerCase() + "%");
		}

		return new FiltroSql(where.toString(), parametros);
	}

	/**
	 * Normaliza la fecha de una consulta nativa a ISO-8601. El driver de PostgreSQL
	 * entrega esta columna como Timestamp si Hibernate no la convierte; usar
	 * String.valueOf produciria un formato distinto al resto de la API.
	 */
	private String fechaIso(Object valor) {
		if (valor == null) {
			return null;
		}
		if (valor instanceof java.sql.Timestamp timestamp) {
			return timestamp.toInstant().toString();
		}
		if (valor instanceof java.time.OffsetDateTime offsetDateTime) {
			return offsetDateTime.toInstant().toString();
		}
		if (valor instanceof java.time.ZonedDateTime zonedDateTime) {
			return zonedDateTime.toInstant().toString();
		}
		if (valor instanceof java.time.LocalDateTime localDateTime) {
			return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toString();
		}
		if (valor instanceof java.time.Instant instant) {
			return instant.toString();
		}
		if (valor instanceof java.util.Date fecha) {
			return fecha.toInstant().toString();
		}
		return valor.toString();
	}

	private Instant inicio(LocalDate fecha) {
		return fecha == null ? null : fecha.atStartOfDay(ZoneId.systemDefault()).toInstant();
	}

	private Instant finExclusivo(LocalDate fecha) {
		return fecha == null ? null : fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
	}

	private record FiltroSql(String where, Map<String, Object> parametros) {
	}
}
