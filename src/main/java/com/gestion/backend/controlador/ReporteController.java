package com.gestion.backend.controlador;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestion.backend.dto.CambioEstadoRequest;
import com.gestion.backend.dto.ReporteCreacionRequest;
import com.gestion.backend.dto.ReporteResponse;
import com.gestion.backend.servicio.ReporteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

	private final ReporteService reporteService;

	public ReporteController(ReporteService reporteService) {
		this.reporteService = reporteService;
	}

	@PostMapping
	public ResponseEntity<ReporteResponse> crear(@Valid @RequestBody ReporteCreacionRequest solicitud) {
		ReporteResponse creado = reporteService.crear(solicitud);
		return ResponseEntity.created(URI.create("/api/reportes/" + creado.id())).body(creado);
	}

	@GetMapping("/{id}")
	public ReporteResponse obtener(@PathVariable Long id) {
		return reporteService.obtener(id);
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMINISTRADOR')")
	public ReporteResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest solicitud) {
		return reporteService.cambiarEstado(id, solicitud.estado());
	}
}
