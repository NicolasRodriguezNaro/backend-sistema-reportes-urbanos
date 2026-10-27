package com.gestion.backend.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException excepcion) throws IOException {
		escribir(response, HttpStatus.UNAUTHORIZED, "No autenticado",
				"Debe enviar un token JWT valido en el header Authorization");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException excepcion) throws IOException {
		escribir(response, HttpStatus.FORBIDDEN, "Acceso denegado",
				"Su rol no tiene permiso para realizar esta operacion");
	}

	private void escribir(HttpServletResponse response, HttpStatus estado, String error, String mensaje)
			throws IOException {
		response.setStatus(estado.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("{\"error\":\"" + error + "\",\"mensaje\":\"" + mensaje + "\"}");
	}
}
