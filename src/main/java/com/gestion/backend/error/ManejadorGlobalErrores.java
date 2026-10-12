package com.gestion.backend.error;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorGlobalErrores {

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RecursoNoEncontradoException excepcion) {
		Map<String, Object> cuerpo = Map.of(
				"error", "No encontrado",
				"mensaje", excepcion.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException excepcion) {
		Map<String, String> campos = new LinkedHashMap<>();
		excepcion.getBindingResult().getFieldErrors()
				.forEach(error -> campos.put(error.getField(), error.getDefaultMessage()));
		Map<String, Object> cuerpo = Map.of(
				"error", "Datos invalidos",
				"campos", campos);
		return ResponseEntity.badRequest().body(cuerpo);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> manejarJsonInvalido(HttpMessageNotReadableException excepcion) {
		Map<String, Object> cuerpo = Map.of(
				"error", "Solicitud malformada",
				"mensaje", "El JSON enviado es invalido o contiene valores no reconocidos");
		return ResponseEntity.badRequest().body(cuerpo);
	}
}
