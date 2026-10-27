package com.gestion.backend.dto;

import com.gestion.backend.modelo.EstadoReporte;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(

		@NotNull(message = "El estado es obligatorio")
		EstadoReporte estado) {
}
