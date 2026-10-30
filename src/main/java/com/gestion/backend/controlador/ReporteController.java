package com.gestion.backend.controlador;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestion.backend.dto.CambioAsignacionRequest;
import com.gestion.backend.dto.CambioEstadoRequest;
import com.gestion.backend.dto.FeatureCollection;
import com.gestion.backend.dto.PaginaResponse;
import com.gestion.backend.dto.ReporteCreacionRequest;
import com.gestion.backend.dto.ReporteResumenResponse;
import com.gestion.backend.dto.ReporteResponse;
import com.gestion.backend.modelo.EstadoReporte;
import com.gestion.backend.modelo.PrioridadCalculada;
import com.gestion.backend.modelo.TipoIncidencia;
import com.gestion.backend.servicio.DashboardService;
import com.gestion.backend.servicio.ReporteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

	private static final String ROLES_GESTION = "hasAnyRole('FUNCIONARIO', 'ADMINISTRADOR', 'EMPRESA')";

	private final ReporteService reporteService;
	private final DashboardService dashboardService;

	public ReporteController(ReporteService reporteService, DashboardService dashboardService) {
		this.reporteService = reporteService;
		this.dashboardService = dashboardService;
	}

	@PostMapping
	public ResponseEntity<ReporteResponse> crear(@Valid @RequestBody ReporteCreacionRequest solicitud) {
		ReporteResponse creado = reporteService.crear(solicitud);
		return ResponseEntity.created(URI.create("/api/reportes/" + creado.id())).body(creado);
	}

	@GetMapping
	@PreAuthorize(ROLES_GESTION)
	public PaginaResponse<ReporteResumenResponse> listar(
			@RequestParam(required = false) EstadoReporte estado,
			@RequestParam(required = false) TipoIncidencia categoria,
			@RequestParam(required = false) PrioridadCalculada prioridad,
			@RequestParam(required = false) Integer barrioId,
			@RequestParam(required = false) Integer entidadId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) String texto,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return reporteService.listar(estado, categoria, prioridad, barrioId, entidadId, desde, hasta, texto,
				page, size);
	}

	@GetMapping("/mios")
	public PaginaResponse<ReporteResumenResponse> listarMios(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return reporteService.listarMios(page, size);
	}

	@GetMapping(value = "/geojson", produces = "application/geo+json")
	@PreAuthorize(ROLES_GESTION)
	public FeatureCollection listarGeoJson(
			@RequestParam(required = false) EstadoReporte estado,
			@RequestParam(required = false) TipoIncidencia categoria,
			@RequestParam(required = false) PrioridadCalculada prioridad,
			@RequestParam(required = false) Integer barrioId,
			@RequestParam(required = false) Integer entidadId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) String texto) {
		return dashboardService.geojsonReportes(desde, hasta, estado, categoria, prioridad, barrioId,
				entidadId, texto);
	}

	@GetMapping("/{id}")
	public ReporteResponse obtener(@PathVariable Long id) {
		return reporteService.obtener(id);
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize(ROLES_GESTION)
	public ReporteResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest solicitud) {
		return reporteService.cambiarEstado(id, solicitud.estado());
	}

	@PatchMapping("/{id}/asignacion")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	public ReporteResponse asignar(@PathVariable Long id, @Valid @RequestBody CambioAsignacionRequest solicitud) {
		return reporteService.asignar(id, solicitud.entidadId());
	}
}
