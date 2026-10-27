package com.gestion.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 150, message = "El nombre no puede superar 150 caracteres")
		String nombre,

		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no es valido")
		@Size(max = 150, message = "El email no puede superar 150 caracteres")
		String email,

		@NotBlank(message = "La contrasena es obligatoria")
		@Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
		String password) {
}
