package com.gestion.backend.dto;

import java.util.List;
import java.util.Map;

import com.gestion.backend.modelo.EstadoReporte;
import com.gestion.backend.modelo.PrioridadCalculada;
import com.gestion.backend.modelo.TipoIncidencia;

public record DashboardResumenResponse(
		long total,
		Map<EstadoReporte, Long> porEstado,
		Map<TipoIncidencia, Long> porCategoria,
		Map<PrioridadCalculada, Long> porPrioridad,
		long criticos,
		long sinAsignar,
		List<ConteoBarrio> porBarrio) {
}
