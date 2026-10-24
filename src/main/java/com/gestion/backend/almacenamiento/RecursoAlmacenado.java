package com.gestion.backend.almacenamiento;

import java.io.InputStream;

public record RecursoAlmacenado(InputStream contenido, String tipoContenido, long tamanio) {
}
