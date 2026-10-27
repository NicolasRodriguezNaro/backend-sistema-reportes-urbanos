package com.gestion.backend.servicio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gestion.backend.almacenamiento.AlmacenamientoArchivos;
import com.gestion.backend.almacenamiento.RecursoAlmacenado;
import com.gestion.backend.dto.ArchivoResponse;

@Service
public class ServicioArchivos {

	private static final Logger log = LoggerFactory.getLogger(ServicioArchivos.class);

	private static final long TAMANIO_MAXIMO_BYTES = 10L * 1024 * 1024;
	private static final int ANCHO_MINIATURA = 320;
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

		byte[] contenido;
		try {
			contenido = archivo.getBytes();
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo leer el archivo subido", e);
		}

		LocalDate hoy = LocalDate.now();
		String clave = String.format("incidentes/%04d/%02d/%02d/%s/hd.%s",
				hoy.getYear(), hoy.getMonthValue(), hoy.getDayOfMonth(), UUID.randomUUID(), extension);

		almacenamiento.guardar(clave, new ByteArrayInputStream(contenido), tipo, contenido.length);

		String claveMiniatura = null;
		String urlMiniatura = null;
		byte[] miniatura = generarMiniatura(contenido);

		if (miniatura != null) {
			claveMiniatura = claveMiniatura(clave);
			almacenamiento.guardar(claveMiniatura, new ByteArrayInputStream(miniatura), "image/jpeg",
					miniatura.length);
			urlMiniatura = almacenamiento.urlDe(claveMiniatura);
		}

		return new ArchivoResponse(clave, almacenamiento.urlDe(clave), claveMiniatura, urlMiniatura);
	}

	public RecursoAlmacenado obtener(String clave) {
		return almacenamiento.obtener(clave);
	}

	private byte[] generarMiniatura(byte[] contenido) {
		try {
			BufferedImage original = ImageIO.read(new ByteArrayInputStream(contenido));
			if (original == null) {
				log.warn("No se pudo interpretar la imagen para generar la miniatura (formato no soportado por ImageIO)");
				return null;
			}

			int ancho = Math.min(ANCHO_MINIATURA, original.getWidth());
			int alto = Math.max(1,
					(int) Math.round(original.getHeight() * (ancho / (double) original.getWidth())));

			BufferedImage miniatura = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
			Graphics2D grafico = miniatura.createGraphics();
			try {
				grafico.setColor(Color.WHITE);
				grafico.fillRect(0, 0, ancho, alto);
				grafico.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				grafico.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				grafico.drawImage(original, 0, 0, ancho, alto, null);
			} finally {
				grafico.dispose();
			}

			ByteArrayOutputStream salida = new ByteArrayOutputStream();
			if (!ImageIO.write(miniatura, "jpg", salida)) {
				log.warn("No se encontro un escritor JPEG para la miniatura");
				return null;
			}
			return salida.toByteArray();
		} catch (IOException e) {
			log.warn("No se pudo generar la miniatura: {}", e.getMessage());
			return null;
		}
	}

	private String claveMiniatura(String claveHd) {
		int barra = claveHd.lastIndexOf('/');
		return claveHd.substring(0, barra + 1) + "thumb.jpg";
	}
}
