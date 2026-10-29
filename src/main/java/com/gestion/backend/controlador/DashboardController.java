package com.gestion.backend.controlador;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestion.backend.dto.DashboardResumenResponse;
import com.gestion.backend.servicio.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/resumen")
	@PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMINISTRADOR', 'EMPRESA')")
	public DashboardResumenResponse resumen(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) Integer barrioId,
			@RequestParam(required = false) Integer entidadId) {
		return dashboardService.resumen(desde, hasta, barrioId, entidadId);
	}
}
