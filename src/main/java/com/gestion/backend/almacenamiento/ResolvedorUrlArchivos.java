package com.gestion.backend.almacenamiento;

import org.springframework.stereotype.Component;

@Component
public class ResolvedorUrlArchivos {

	private final AlmacenamientoArchivos almacenamiento;

	public ResolvedorUrlArchivos(AlmacenamientoArchivos almacenamiento) {
		this.almacenamiento = almacenamiento;
	}

	public String resolver(String valor) {
		if (valor == null) {
			return null;
		}
		if (valor.startsWith("http://") || valor.startsWith("https://")) {
			return valor;
		}
		return almacenamiento.urlDe(valor);
	}
}
