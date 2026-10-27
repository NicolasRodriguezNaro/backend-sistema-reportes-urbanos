package com.gestion.backend.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestion.backend.dto.LoginRequest;
import com.gestion.backend.dto.RegistroRequest;
import com.gestion.backend.dto.TokenResponse;
import com.gestion.backend.dto.UsuarioResumen;
import com.gestion.backend.servicio.ServicioAutenticacion;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final ServicioAutenticacion servicioAutenticacion;

	public AuthController(ServicioAutenticacion servicioAutenticacion) {
		this.servicioAutenticacion = servicioAutenticacion;
	}

	@PostMapping("/registro")
	public ResponseEntity<TokenResponse> registrar(@Valid @RequestBody RegistroRequest solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED).body(servicioAutenticacion.registrar(solicitud));
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest solicitud) {
		return servicioAutenticacion.login(solicitud);
	}

	@GetMapping("/yo")
	public UsuarioResumen yo() {
		return servicioAutenticacion.actual();
	}
}
