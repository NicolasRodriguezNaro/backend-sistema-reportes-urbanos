package com.gestion.backend.controlador;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestion.backend.dto.EntidadResumenResponse;
import com.gestion.backend.servicio.CatalogoService;

@RestController
public class CatalogoController {

	private static final MediaType GEOJSON = MediaType.valueOf("application/geo+json");

	private final CatalogoService catalogoService;

	public CatalogoController(CatalogoService catalogoService) {
		this.catalogoService = catalogoService;
	}

	@GetMapping(value = "/api/barrios/geojson", produces = "application/geo+json")
	@PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMINISTRADOR', 'EMPRESA')")
	public ResponseEntity<String> barriosGeoJson() {
		return ResponseEntity.ok().contentType(GEOJSON).body(catalogoService.barriosGeoJson());
	}

	@GetMapping(value = "/api/sitios-interes/geojson", produces = "application/geo+json")
	@PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMINISTRADOR', 'EMPRESA')")
	public ResponseEntity<String> sitiosGeoJson() {
		return ResponseEntity.ok().contentType(GEOJSON).body(catalogoService.sitiosGeoJson());
	}

	@GetMapping("/api/entidades")
	@PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMINISTRADOR', 'EMPRESA')")
	public List<EntidadResumenResponse> entidades() {
		return catalogoService.entidades();
	}
}
