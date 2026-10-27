package com.gestion.backend.repositorio;

import java.time.Instant;

import com.gestion.backend.modelo.EstadoReporte;
import com.gestion.backend.modelo.PrioridadCalculada;
import com.gestion.backend.modelo.TipoIncidencia;

public record ReporteFiltros(
		EstadoReporte estado,
		TipoIncidencia categoria,
		PrioridadCalculada prioridad,
		Integer barrioId,
		Integer entidadId,
		Instant desde,
		Instant hasta,
		String texto) {
}
