package com.gestion.backend.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.gestion.backend.modelo.Reporte;

public interface ReporteRepository extends JpaRepository<Reporte, Long>, JpaSpecificationExecutor<Reporte> {

	@Override
	@EntityGraph(attributePaths = { "usuario", "barrio", "entidadAsignada", "reportePadre" })
	Page<Reporte> findAll(Specification<Reporte> spec, Pageable pageable);

	@EntityGraph(attributePaths = { "usuario", "barrio", "entidadAsignada", "reportePadre" })
	Page<Reporte> findByUsuarioId(Long usuarioId, Pageable pageable);
}
