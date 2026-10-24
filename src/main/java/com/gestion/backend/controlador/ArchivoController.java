package com.gestion.backend.controlador;

import java.net.URI;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gestion.backend.almacenamiento.RecursoAlmacenado;
import com.gestion.backend.dto.ArchivoResponse;
import com.gestion.backend.servicio.ServicioArchivos;

@RestController
@RequestMapping("/api/archivos")
public class ArchivoController {

	private final ServicioArchivos servicioArchivos;

	public ArchivoController(ServicioArchivos servicioArchivos) {
		this.servicioArchivos = servicioArchivos;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ArchivoResponse> subir(@RequestPart("archivo") MultipartFile archivo) {
		ArchivoResponse respuesta = servicioArchivos.subir(archivo);
		return ResponseEntity.created(URI.create(respuesta.url())).body(respuesta);
	}

	@GetMapping("/{*clave}")
	public ResponseEntity<InputStreamResource> obtener(@PathVariable String clave) {
		String ruta = clave.startsWith("/") ? clave.substring(1) : clave;
		RecursoAlmacenado recurso = servicioArchivos.obtener(ruta);

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(recurso.tipoContenido()))
				.contentLength(recurso.tamanio())
				.body(new InputStreamResource(recurso.contenido()));
	}
}
