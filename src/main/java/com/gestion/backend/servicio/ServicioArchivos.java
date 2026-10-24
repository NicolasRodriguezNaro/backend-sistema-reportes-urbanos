package com.gestion.backend.servicio;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gestion.backend.almacenamiento.AlmacenamientoArchivos;
import com.gestion.backend.almacenamiento.RecursoAlmacenado;
import com.gestion.backend.dto.ArchivoResponse;

@Service
public class ServicioArchivos {

	private static final long TAMANIO_MAXIMO_BYTES = 10L * 1024 * 1024;
	private static final Map<String, String> EXTENSIONES_POR_TIPO = Map.of(
			"image/jpeg", "jpg",
			"image/png", "png",
			"image/webp", "webp");

	private final AlmacenamientoArchivos almacenamiento;

	public ServicioArchivos(AlmacenamientoArchivos almacenamiento) {
		this.almacenamiento = almacenamiento;
	}

	public ArchivoResponse subir(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			throw new IllegalArgumentException("El archivo esta vacio");
		}
		if (archivo.getSize() > TAMANIO_MAXIMO_BYTES) {
			throw new IllegalArgumentException("La imagen no puede superar los 10 MB");
		}

		String tipo = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
		String extension = EXTENSIONES_POR_TIPO.get(tipo);
		if (extension == null) {
			throw new IllegalArgumentException("Formato no permitido. Se aceptan JPEG, PNG o WEBP");
		}

		LocalDate hoy = LocalDate.now();
		String clave = String.format("incidentes/%04d/%02d/%02d/%s/hd.%s",
				hoy.getYear(), hoy.getMonthValue(), hoy.getDayOfMonth(), UUID.randomUUID(), extension);

		try (InputStream contenido = archivo.getInputStream()) {
			almacenamiento.guardar(clave, contenido, tipo, archivo.getSize());
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo leer el archivo subido", e);
		}

		return new ArchivoResponse(clave, almacenamiento.urlDe(clave));
	}

	public RecursoAlmacenado obtener(String clave) {
		return almacenamiento.obtener(clave);
	}
}
