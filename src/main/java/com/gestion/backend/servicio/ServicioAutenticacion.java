package com.gestion.backend.servicio;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.backend.dto.LoginRequest;
import com.gestion.backend.dto.RegistroRequest;
import com.gestion.backend.dto.TokenResponse;
import com.gestion.backend.dto.UsuarioResumen;
import com.gestion.backend.error.CredencialesInvalidasException;
import com.gestion.backend.modelo.RolUsuario;
import com.gestion.backend.modelo.Usuario;
import com.gestion.backend.repositorio.UsuarioRepository;
import com.gestion.backend.seguridad.ServicioTokens;
import com.gestion.backend.seguridad.ServicioUsuarioActual;

@Service
public class ServicioAutenticacion {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final ServicioTokens servicioTokens;
	private final ServicioUsuarioActual servicioUsuarioActual;

	public ServicioAutenticacion(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			ServicioTokens servicioTokens, ServicioUsuarioActual servicioUsuarioActual) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.servicioTokens = servicioTokens;
		this.servicioUsuarioActual = servicioUsuarioActual;
	}

	@Transactional
	public TokenResponse registrar(RegistroRequest solicitud) {
		String email = solicitud.email().trim().toLowerCase();
		if (usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new IllegalArgumentException("Ya existe un usuario registrado con ese email");
		}

		Usuario usuario = new Usuario();
		usuario.setNombre(solicitud.nombre().trim());
		usuario.setEmail(email);
		usuario.setPasswordHash(passwordEncoder.encode(solicitud.password()));
		usuario.setRol(RolUsuario.CIUDADANO);
		usuario.setFechaRegistro(Instant.now());
		usuario = usuarioRepository.save(usuario);

		return respuesta(usuario);
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest solicitud) {
		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(solicitud.email().trim())
				.orElseThrow(CredencialesInvalidasException::new);

		if (!passwordEncoder.matches(solicitud.password(), usuario.getPasswordHash())) {
			throw new CredencialesInvalidasException();
		}

		return respuesta(usuario);
	}

	@Transactional(readOnly = true)
	public UsuarioResumen actual() {
		return UsuarioResumen.desde(servicioUsuarioActual.actual());
	}

	private TokenResponse respuesta(Usuario usuario) {
		ServicioTokens.TokenGenerado generado = servicioTokens.generar(usuario);
		return new TokenResponse(generado.token(), "Bearer", generado.expiraEnSegundos(),
				UsuarioResumen.desde(usuario));
	}
}
