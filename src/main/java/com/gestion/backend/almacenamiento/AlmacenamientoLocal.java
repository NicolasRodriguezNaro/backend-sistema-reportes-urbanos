package com.gestion.backend.almacenamiento;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.gestion.backend.error.RecursoNoEncontradoException;

@Service
@Profile("dev & !minio")
public class AlmacenamientoLocal implements AlmacenamientoArchivos {

	private static final Map<String, String> TIPOS_POR_EXTENSION = Map.of(
			"jpg", "image/jpeg",
			"jpeg", "image/jpeg",
			"png", "image/png",
			"webp", "image/webp");

	private final Path raiz;
	private final String urlBase;

	public AlmacenamientoLocal(
			@Value("${gestion.almacenamiento.ruta-local:uploads}") String rutaLocal,
			@Value("${gestion.almacenamiento.url-base:http://localhost:8080}") String urlBase) {
		this.raiz = Path.of(rutaLocal).toAbsolutePath().normalize();
		this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
	}

	@Override
	public void guardar(String clave, InputStream contenido, String tipoContenido, long tamanio) {
		Path destino = resolver(clave);
		try {
			Files.createDirectories(destino.getParent());
			Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo guardar el archivo en el almacenamiento local", e);
		}
	}

	@Override
	public String urlDe(String clave) {
		return urlBase + "/api/archivos/" + clave;
	}

	@Override
	public RecursoAlmacenado obtener(String clave) {
		Path archivo = resolver(clave);
		if (!Files.isRegularFile(archivo)) {
			throw new RecursoNoEncontradoException("Archivo no encontrado: " + clave);
		}
		try {
			return new RecursoAlmacenado(Files.newInputStream(archivo), tipoDe(archivo), Files.size(archivo));
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo leer el archivo del almacenamiento local", e);
		}
	}

	private Path resolver(String clave) {
		if (clave == null || clave.isBlank() || clave.contains("..")) {
			throw new IllegalArgumentException("Clave de archivo invalida");
		}
		Path destino = raiz.resolve(clave).normalize();
		if (!destino.startsWith(raiz)) {
			throw new IllegalArgumentException("Clave de archivo invalida");
		}
		return destino;
	}

	private String tipoDe(Path archivo) {
		String nombre = archivo.getFileName().toString();
		int punto = nombre.lastIndexOf('.');
		if (punto >= 0) {
			String tipo = TIPOS_POR_EXTENSION.get(nombre.substring(punto + 1).toLowerCase());
			if (tipo != null) {
				return tipo;
			}
		}
		return "application/octet-stream";
	}
}
