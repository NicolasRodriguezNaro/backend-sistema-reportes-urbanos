package com.gestion.backend.dto;

import java.util.List;

public record PaginaResponse<T>(
		List<T> contenido,
		int pagina,
		int tamano,
		long total,
		int totalPaginas) {
}
