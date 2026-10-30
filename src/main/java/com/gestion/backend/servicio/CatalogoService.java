package com.gestion.backend.servicio;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.backend.dto.EntidadResumenResponse;
import com.gestion.backend.repositorio.EntidadRepository;
import com.gestion.backend.seguridad.AlcanceReportes;

import jakarta.persistence.EntityManager;

@Service
public class CatalogoService {

	private final EntityManager entityManager;
	private final EntidadRepository entidadRepository;
	private final AlcanceReportes alcanceReportes;

	public CatalogoService(EntityManager entityManager, EntidadRepository entidadRepository,
			AlcanceReportes alcanceReportes) {
		this.entityManager = entityManager;
		this.entidadRepository = entidadRepository;
		this.alcanceReportes = alcanceReportes;
	}

	@Transactional(readOnly = true)
	public String barriosGeoJson() {
		return (String) entityManager.createNativeQuery("""
				SELECT json_build_object(
					'type', 'FeatureCollection',
					'features', COALESCE(json_agg(json_build_object(
						'type', 'Feature',
						'geometry', ST_AsGeoJSON(ST_Transform(geom, 4326))::json,
						'properties', json_build_object(
							'gid', gid,
							'nombre', nombre,
							'codComuna', cod_comuna,
							'codBarrio', cod_barrio
						)
					)), '[]'::json)
				)::text
				FROM barrios
				""").getSingleResult();
	}

	@Transactional(readOnly = true)
	public String sitiosGeoJson() {
		return (String) entityManager.createNativeQuery("""
				SELECT json_build_object(
					'type', 'FeatureCollection',
					'features', COALESCE(json_agg(json_build_object(
						'type', 'Feature',
						'geometry', ST_AsGeoJSON(ST_Transform(geom, 4326))::json,
						'properties', json_build_object(
							'gid', gid,
							'nombre', nombre,
							'tipoSitio', tipo_sitio,
							'radioInfluencia', radio_influencia,
							'critico', tipo_sitio IN ('HOSPITAL', 'INSTITUCION_EDUCATIVA', 'UNIVERSIDAD', 'VIA_ARTERIA')
						)
					)), '[]'::json)
				)::text
				FROM sitios_interes
				""").getSingleResult();
	}

	@Transactional(readOnly = true)
	public List<EntidadResumenResponse> entidades() {
		var usuario = alcanceReportes.usuarioActual();

		if (alcanceReportes.esAdministrador(usuario)) {
			return entidadRepository.findAll(Sort.by("id")).stream()
					.map(this::aResumen)
					.toList();
		}

		Integer entidadId = alcanceReportes.entidadIdObligatoria(usuario);
		return entidadRepository.findById(entidadId).stream()
				.map(this::aResumen)
				.toList();
	}

	private EntidadResumenResponse aResumen(com.gestion.backend.modelo.Entidad entidad) {
		return new EntidadResumenResponse(entidad.getId(), entidad.getNombre(),
				entidad.getTipo(), entidad.getContactoEmail());
	}
}
