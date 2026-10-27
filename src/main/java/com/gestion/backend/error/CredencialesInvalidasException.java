package com.gestion.backend.error;

public class CredencialesInvalidasException extends RuntimeException {

	public CredencialesInvalidasException() {
		super("Email o contrasena incorrectos");
	}
}
