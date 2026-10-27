package com.gestion.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.backend.modelo.RolUsuario;
import com.gestion.backend.modelo.Usuario;
import com.gestion.backend.repositorio.UsuarioRepository;
import com.gestion.backend.seguridad.ServicioTokens;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {
		"gestion.semantica.ruta-tdb=target/jena-test-web",
		"gestion.almacenamiento.ruta-local=target/uploads-test-web" })
@AutoConfigureMockMvc
@Transactional
class ArchivoTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private ServicioTokens servicioTokens;

	private Usuario crearUsuarioDePrueba() {
		Usuario usuario = new Usuario();
		usuario.setNombre("Usuario Archivos");
		usuario.setEmail("prueba.archivos@test.local");
		usuario.setPasswordHash("no-aplica");
		usuario.setRol(RolUsuario.CIUDADANO);
		return usuarioRepository.save(usuario);
	}

	private String autorizacion(Usuario usuario) {
		return "Bearer " + servicioTokens.generar(usuario).token();
	}

	private byte[] imagenDePrueba() throws IOException {
		BufferedImage imagen = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		Graphics2D grafico = imagen.createGraphics();
		try {
			grafico.setColor(new Color(30, 120, 200));
			grafico.fillRect(0, 0, 640, 480);
			grafico.setColor(Color.WHITE);
			grafico.fillOval(160, 120, 320, 240);
		} finally {
			grafico.dispose();
		}
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		ImageIO.write(imagen, "jpg", salida);
		return salida.toByteArray();
	}

	private MvcResult subirImagen(String autorizacion, byte[] imagen) throws Exception {
		MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", imagen);
		return mockMvc.perform(multipart("/api/archivos")
				.file(archivo)
				.header("Authorization", autorizacion))
				.andExpect(status().isCreated())
				.andReturn();
	}

	@Test
	void subirImagenDevuelveClaveYUrlAccesibleConMiniatura() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);
		byte[] imagen = imagenDePrueba();

		MvcResult subida = subirImagen(autorizacion, imagen);

		String cuerpo = subida.getResponse().getContentAsString();
		String clave = JsonPath.read(cuerpo, "$.clave");
		String url = JsonPath.read(cuerpo, "$.url");
		String claveThumb = JsonPath.read(cuerpo, "$.claveThumb");
		String urlThumb = JsonPath.read(cuerpo, "$.urlThumb");

		assertThat(clave).startsWith("incidentes/").endsWith(".jpg");
		assertThat(url).contains(clave);
		assertThat(claveThumb).startsWith("incidentes/").endsWith("thumb.jpg");
		assertThat(urlThumb).contains(claveThumb);

		mockMvc.perform(get(URI.create(url).getPath())
				.header("Authorization", autorizacion))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andExpect(content().bytes(imagen));

		MvcResult descargaMiniatura = mockMvc.perform(get(URI.create(urlThumb).getPath())
				.header("Authorization", autorizacion))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andReturn();

		BufferedImage miniatura = ImageIO.read(
				new ByteArrayInputStream(descargaMiniatura.getResponse().getContentAsByteArray()));
		assertThat(miniatura).isNotNull();
		assertThat(miniatura.getWidth()).isEqualTo(320);
		assertThat(miniatura.getHeight()).isEqualTo(240);
	}

	@Test
	void archivoQueNoEsImagenResponde400() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		MockMultipartFile archivo = new MockMultipartFile("archivo", "nota.txt", "text/plain",
				"hola".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/archivos")
				.file(archivo)
				.header("Authorization", autorizacion))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Solicitud invalida"));
	}

	@Test
	void archivoVacioResponde400() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		MockMultipartFile archivo = new MockMultipartFile("archivo", "vacio.jpg", "image/jpeg", new byte[0]);

		mockMvc.perform(multipart("/api/archivos")
				.file(archivo)
				.header("Authorization", autorizacion))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Solicitud invalida"));
	}

	@Test
	void reporteCreadoConClavesSubidasDevuelveUrlsResueltas() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		MvcResult subida = subirImagen(autorizacion, imagenDePrueba());
		String cuerpoSubida = subida.getResponse().getContentAsString();
		String clave = JsonPath.read(cuerpoSubida, "$.clave");
		String claveThumb = JsonPath.read(cuerpoSubida, "$.claveThumb");

		String solicitud = """
				{
				  "categoriaIa": "FUGA_AGUA",
				  "severidadIa": "BAJA",
				  "latitud": 3.4516,
				  "longitud": -76.5320,
				  "claveImagenHd": "%s",
				  "claveImagenThumb": "%s"
				}
				""".formatted(clave, claveThumb);

		MvcResult creacion = mockMvc.perform(post("/api/reportes")
				.header("Authorization", autorizacion)
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitud))
				.andExpect(status().isCreated())
				.andReturn();

		String cuerpo = creacion.getResponse().getContentAsString();
		assertThat((String) JsonPath.read(cuerpo, "$.claveImagenHd")).isEqualTo(clave);
		assertThat((String) JsonPath.read(cuerpo, "$.urlImagenHd")).contains(clave);
		assertThat((String) JsonPath.read(cuerpo, "$.claveImagenThumb")).isEqualTo(claveThumb);
		assertThat((String) JsonPath.read(cuerpo, "$.urlImagenThumb")).contains(claveThumb);
	}
}
