package com.gestion.backend.almacenamiento;

import java.io.InputStream;

public interface AlmacenamientoArchivos {

	void guardar(String clave, InputStream contenido, String tipoContenido, long tamanio);

	String urlDe(String clave);

	RecursoAlmacenado obtener(String clave);
}
