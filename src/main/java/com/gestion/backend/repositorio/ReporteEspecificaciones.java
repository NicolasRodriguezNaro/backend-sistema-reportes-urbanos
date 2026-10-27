package com.gestion.backend.repositorio;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.gestion.backend.modelo.Reporte;

import jakarta.persistence.criteria.Predicate;

public final class ReporteEspecificaciones {

	private ReporteEspecificaciones() {
	}

	public static Specification<Reporte> conFiltros(ReporteFiltros filtros) {
		return (raiz, consulta, cb) -> {
			List<Predicate> predicados = new ArrayList<>();

			if (filtros.estado() != null) {
				predicados.add(cb.equal(raiz.get("estado"), filtros.estado()));
			}
			if (filtros.categoria() != null) {
				predicados.add(cb.equal(raiz.get("categoriaIa"), filtros.categoria()));
			}
			if (filtros.prioridad() != null) {
				predicados.add(cb.equal(raiz.get("prioridadCalculada"), filtros.prioridad()));
			}
			if (filtros.barrioId() != null) {
				predicados.add(cb.equal(raiz.get("barrio").get("gid"), filtros.barrioId()));
			}
			if (filtros.entidadId() != null) {
				predicados.add(cb.equal(raiz.get("entidadAsignada").get("id"), filtros.entidadId()));
			}
			if (filtros.desde() != null) {
				predicados.add(cb.greaterThanOrEqualTo(raiz.get("fechaCreacion"), filtros.desde()));
			}
			if (filtros.hasta() != null) {
				predicados.add(cb.lessThan(raiz.get("fechaCreacion"), filtros.hasta()));
			}
			if (filtros.texto() != null && !filtros.texto().isBlank()) {
				predicados.add(cb.like(cb.lower(raiz.get("descripcion")),
						"%" + filtros.texto().trim().toLowerCase() + "%"));
			}

			return predicados.isEmpty() ? cb.conjunction() : cb.and(predicados.toArray(new Predicate[0]));
		};
	}
}
