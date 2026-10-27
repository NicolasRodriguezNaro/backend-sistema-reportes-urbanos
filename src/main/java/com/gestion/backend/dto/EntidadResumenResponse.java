package com.gestion.backend.dto;

import com.gestion.backend.modelo.TipoEntidad;

public record EntidadResumenResponse(
		Integer id,
		String nombre,
		TipoEntidad tipo,
		String contactoEmail) {
}
