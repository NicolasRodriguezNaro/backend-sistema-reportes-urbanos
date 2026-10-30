package com.gestion.backend.dto;

import java.time.Instant;

import com.gestion.backend.modelo.EstadoReporte;
import com.gestion.backend.modelo.PrioridadCalculada;
import com.gestion.backend.modelo.Severidad;
import com.gestion.backend.modelo.TipoIncidencia;

public record ReporteResponse(

		Long id,

		EstadoReporte estado,

		TipoIncidencia categoriaIa,

		Severidad severidadIa,

		PrioridadCalculada prioridadCalculada,

		Double confianzaIa,

		String descripcion,

		double latitud,

		double longitud,

		String claveImagenHd,

		String urlImagenHd,

		String claveImagenThumb,

		String urlImagenThumb,

		Instant fechaCreacion,

		Instant fechaActualizacion,

		Long usuarioId,

		Integer barrioId,

		String barrioNombre,

		Integer entidadAsignadaId,

		String entidadNombre,

		Long reportePadreId) {
}
