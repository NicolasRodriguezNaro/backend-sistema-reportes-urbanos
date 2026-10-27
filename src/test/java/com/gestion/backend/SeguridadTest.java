package com.gestion.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class SeguridadTest {

	private static final String PASSWORD = "Password123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ServicioTokens servicioTokens;

	private String emailUnico() {
		return "seguridad." + UUID.randomUUID() + "@test.local";
	}

	private MvcResult registrar(String email) throws Exception {
		String solicitud = """
				{ "nombre": "Usuario Seguridad", "email": "%s", "password": "%s" }
				""".formatted(email, PASSWORD);

		return mockMvc.perform(post("/api/auth/registro")
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitud))
				.andExpect(status().isCreated())
				.andReturn();
	}

	private String tokenDeRegistro(MvcResult registro) throws Exception {
		return "Bearer " + JsonPath.read(registro.getResponse().getContentAsString(), "$.token");
	}

	private String solicitudReporte(String claveImagen) {
		return """
				{
				  "categoriaIa": "FUGA_AGUA",
				  "severidadIa": "MEDIA",
				  "latitud": 3.4516,
				  "longitud": -76.5320,
				  "claveImagenHd": "%s"
				}
				""".formatted(claveImagen);
	}

	private MvcResult crearReporte(String autorizacion) throws Exception {
		return mockMvc.perform(post("/api/reportes")
				.header("Authorization", autorizacion)
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitudReporte("incidentes/prueba/hd.jpg")))
				.andExpect(status().isCreated())
				.andReturn();
	}

	@Test
	void registroYLoginDevuelvenToken() throws Exception {
		String email = emailUnico();
		MvcResult registro = registrar(email);

		String cuerpo = registro.getResponse().getContentAsString();
		assertThat((String) JsonPath.read(cuerpo, "$.token")).isNotBlank();
		assertThat((String) JsonPath.read(cuerpo, "$.usuario.rol")).isEqualTo("CIUDADANO");

		String login = """
				{ "email": "%s", "password": "%s" }
				""".formatted(email, PASSWORD);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(login))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());

		String loginIncorrecto = """
				{ "email": "%s", "password": "Incorrecta123" }
				""".formatted(email);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginIncorrecto))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sinTokenResponde401() throws Exception {
		mockMvc.perform(get("/api/reportes/1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void yoRequiereToken() throws Exception {
		mockMvc.perform(get("/api/auth/yo"))
				.andExpect(status().isUnauthorized());

		MvcResult registro = registrar(emailUnico());
		String autorizacion = tokenDeRegistro(registro);

		mockMvc.perform(get("/api/auth/yo")
				.header("Authorization", autorizacion))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").exists());
	}

	@Test
	void tokenInvalidoResponde401() throws Exception {
		mockMvc.perform(get("/api/reportes/1")
				.header("Authorization", "Bearer token-falso"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void ciudadanoNoPuedeCambiarEstado() throws Exception {
		MvcResult registro = registrar(emailUnico());
		String autorizacion = tokenDeRegistro(registro);

		MvcResult creacion = crearReporte(autorizacion);
		Number id = JsonPath.read(creacion.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(patch("/api/reportes/" + id.longValue() + "/estado")
				.header("Authorization", autorizacion)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"EN_REVISION\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void funcionarioPuedeCambiarEstado() throws Exception {
		MvcResult registro = registrar(emailUnico());
		String autorizacionCiudadano = tokenDeRegistro(registro);

		MvcResult creacion = crearReporte(autorizacionCiudadano);
		Number id = JsonPath.read(creacion.getResponse().getContentAsString(), "$.id");

		Usuario funcionario = new Usuario();
		funcionario.setNombre("Funcionario Prueba");
		funcionario.setEmail(emailUnico());
		funcionario.setPasswordHash(passwordEncoder.encode(PASSWORD));
		funcionario.setRol(RolUsuario.FUNCIONARIO);
		funcionario.setFechaRegistro(Instant.now());
		funcionario = usuarioRepository.save(funcionario);
		String autorizacionFuncionario = "Bearer " + servicioTokens.generar(funcionario).token();

		mockMvc.perform(patch("/api/reportes/" + id.longValue() + "/estado")
				.header("Authorization", autorizacionFuncionario)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"estado\":\"EN_REVISION\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("EN_REVISION"));
	}

	@Test
	void reporteUsaElUsuarioDelToken() throws Exception {
		MvcResult registro = registrar(emailUnico());
		String autorizacion = tokenDeRegistro(registro);

		Number usuarioId = JsonPath.read(registro.getResponse().getContentAsString(), "$.usuario.id");

		MvcResult creacion = crearReporte(autorizacion);
		Number reporteUsuarioId = JsonPath.read(creacion.getResponse().getContentAsString(), "$.usuarioId");

		assertThat(reporteUsuarioId.intValue()).isEqualTo(usuarioId.intValue());
	}
}
