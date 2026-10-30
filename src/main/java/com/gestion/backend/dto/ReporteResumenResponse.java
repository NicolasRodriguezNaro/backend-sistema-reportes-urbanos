package com.gestion.backend.dto;

import java.time.Instant;

import com.gestion.backend.modelo.EstadoReporte;
import com.gestion.backend.modelo.PrioridadCalculada;
import com.gestion.backend.modelo.Severidad;
import com.gestion.backend.modelo.TipoIncidencia;

public record ReporteResumenResponse(
		Long id,
		EstadoReporte estado,
		TipoIncidencia categoriaIa,
		Severidad severidadIa,
		PrioridadCalculada prioridadCalculada,
		String descripcion,
		String claveImagenThumb,
		String urlImagenThumb,
		Integer barrioId,
		String barrioNombre,
		Integer entidadAsignadaId,
		String entidadNombre,
		Long usuarioId,
		Long reportePadreId,
		Instant fechaCreacion,
		Instant fechaActualizacion) {
}
