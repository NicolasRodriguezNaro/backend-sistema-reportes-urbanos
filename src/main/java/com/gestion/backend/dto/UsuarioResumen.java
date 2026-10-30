package com.gestion.backend.dto;

import com.gestion.backend.modelo.RolUsuario;
import com.gestion.backend.modelo.Usuario;

public record UsuarioResumen(Long id, String nombre, String email, RolUsuario rol, Integer entidadId,
		String entidadNombre) {

	public static UsuarioResumen desde(Usuario usuario) {
		return new UsuarioResumen(
				usuario.getId(),
				usuario.getNombre(),
				usuario.getEmail(),
				usuario.getRol(),
				usuario.getEntidad() != null ? usuario.getEntidad().getId() : null,
				usuario.getEntidad() != null ? usuario.getEntidad().getNombre() : null);
	}
}
