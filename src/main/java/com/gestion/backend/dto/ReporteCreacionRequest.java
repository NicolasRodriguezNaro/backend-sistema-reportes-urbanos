package com.gestion.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.gestion.backend.modelo.Severidad;
import com.gestion.backend.modelo.TipoIncidencia;

public record ReporteCreacionRequest(

		@Size(max = 2000, message = "La descripcion no puede superar 2000 caracteres")
		String descripcion,

		TipoIncidencia categoriaIa,

		Severidad severidadIa,

		@NotNull(message = "La latitud es obligatoria")
		@DecimalMin(value = "-90.0", message = "La latitud minima es -90")
		@DecimalMax(value = "90.0", message = "La latitud maxima es 90")
		Double latitud,

		@NotNull(message = "La longitud es obligatoria")
		@DecimalMin(value = "-180.0", message = "La longitud minima es -180")
		@DecimalMax(value = "180.0", message = "La longitud maxima es 180")
		Double longitud,

		@NotBlank(message = "La clave de la imagen HD es obligatoria")
		@Size(max = 500, message = "La clave de la imagen HD no puede superar 500 caracteres")
		String claveImagenHd,

		@Size(max = 500, message = "La clave de la miniatura no puede superar 500 caracteres")
		String claveImagenThumb) {
}
