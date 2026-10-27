package com.gestion.backend.seguridad;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.gestion.backend.modelo.Reporte;
import com.gestion.backend.modelo.RolUsuario;
import com.gestion.backend.modelo.Usuario;

@Component
public class AlcanceReportes {

	private final ServicioUsuarioActual servicioUsuarioActual;

	public AlcanceReportes(ServicioUsuarioActual servicioUsuarioActual) {
		this.servicioUsuarioActual = servicioUsuarioActual;
	}

	public Usuario usuarioActual() {
		return servicioUsuarioActual.actual();
	}

	public boolean esAdministrador(Usuario usuario) {
		return usuario.getRol() == RolUsuario.ADMINISTRADOR;
	}

	public Integer entidadIdObligatoria(Usuario usuario) {
		if (esAdministrador(usuario)) {
			return null;
		}
		if (usuario.getEntidad() == null) {
			throw new AccessDeniedException("El usuario no tiene una entidad asignada");
		}
		return usuario.getEntidad().getId();
	}

	public void verificarPuedeVer(Usuario usuario, Reporte reporte) {
		if (esAdministrador(usuario)) {
			return;
		}
		if (usuario.getRol() == RolUsuario.CIUDADANO) {
			if (!reporte.getUsuario().getId().equals(usuario.getId())) {
				throw new AccessDeniedException("No tiene acceso a este reporte");
			}
			return;
		}
		verificarEntidad(usuario, reporte);
	}

	public void verificarPuedeGestionar(Usuario usuario, Reporte reporte) {
		if (esAdministrador(usuario)) {
			return;
		}
		verificarEntidad(usuario, reporte);
	}

	public void verificarPuedeAsignar(Usuario usuario) {
		if (!esAdministrador(usuario)) {
			throw new AccessDeniedException("Solo un administrador puede reasignar reportes");
		}
	}

	private void verificarEntidad(Usuario usuario, Reporte reporte) {
		Integer entidad = entidadIdObligatoria(usuario);
		if (reporte.getEntidadAsignada() == null
				|| !entidad.equals(reporte.getEntidadAsignada().getId())) {
			throw new AccessDeniedException("El reporte no pertenece a su entidad");
		}
	}
}
