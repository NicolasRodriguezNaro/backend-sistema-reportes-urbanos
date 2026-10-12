package com.gestion.backend.modelo;

public enum TipoIncidencia {

	HUECO_VIA("Hueco en la via"),
	FUGA_AGUA("Fuga de agua"),
	FALLA_ALUMBRADO("Falla de alumbrado"),
	ACUMULACION_BASURA("Acumulacion de basura");

	private final String descripcion;

	TipoIncidencia(String descripcion) {
		this.descripcion = descripcion;
	}

	public String getDescripcion() {
		return descripcion;
	}
}
