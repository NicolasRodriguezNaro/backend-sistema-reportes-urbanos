package com.gestion.backend.dto;

import jakarta.validation.constraints.NotNull;

public record CambioAsignacionRequest(

		@NotNull(message = "La entidad es obligatoria")
		Integer entidadId) {
}
