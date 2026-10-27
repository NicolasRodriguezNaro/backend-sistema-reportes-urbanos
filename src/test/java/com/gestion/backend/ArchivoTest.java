package com.gestion.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.charset.StandardCharsets;

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

	private static final byte[] IMAGEN = new byte[] {
			(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F' };

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

	private MvcResult subirImagen(String autorizacion) throws Exception {
		MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", IMAGEN);
		return mockMvc.perform(multipart("/api/archivos")
				.file(archivo)
				.header("Authorization", autorizacion))
				.andExpect(status().isCreated())
				.andReturn();
	}

	@Test
	void subirImagenDevuelveClaveYUrlAccesible() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		MvcResult subida = subirImagen(autorizacion);

		String cuerpo = subida.getResponse().getContentAsString();
		String clave = JsonPath.read(cuerpo, "$.clave");
		String url = JsonPath.read(cuerpo, "$.url");

		assertThat(clave).startsWith("incidentes/").endsWith(".jpg");
		assertThat(url).contains(clave);

		mockMvc.perform(get(URI.create(url).getPath())
				.header("Authorization", autorizacion))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andExpect(content().bytes(IMAGEN));
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
	void reporteCreadoConClaveSubidaDevuelveUrlResuelta() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		MvcResult subida = subirImagen(autorizacion);
		String clave = JsonPath.read(subida.getResponse().getContentAsString(), "$.clave");

		String solicitud = """
				{
				  "categoriaIa": "FUGA_AGUA",
				  "severidadIa": "BAJA",
				  "latitud": 3.4516,
				  "longitud": -76.5320,
				  "claveImagenHd": "%s"
				}
				""".formatted(clave);

		MvcResult creacion = mockMvc.perform(post("/api/reportes")
				.header("Authorization", autorizacion)
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitud))
				.andExpect(status().isCreated())
				.andReturn();

		String cuerpo = creacion.getResponse().getContentAsString();
		assertThat((String) JsonPath.read(cuerpo, "$.claveImagenHd")).isEqualTo(clave);
		assertThat((String) JsonPath.read(cuerpo, "$.urlImagenHd")).contains(clave);
	}
}
