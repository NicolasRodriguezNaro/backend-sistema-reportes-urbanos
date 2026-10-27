package com.gestion.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.backend.modelo.RolUsuario;
import com.gestion.backend.modelo.Usuario;
import com.gestion.backend.repositorio.UsuarioRepository;
import com.gestion.backend.seguridad.ServicioTokens;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
		"gestion.semantica.ruta-tdb=target/jena-test-web",
		"gestion.almacenamiento.ruta-local=target/uploads-test-web" })
@AutoConfigureMockMvc
@Transactional
class ReporteApiTest {

	private static final double LATITUD_BASE = 0.0;
	private static final double LONGITUD_BASE = -70.0;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ServicioTokens servicioTokens;

	private Usuario crearUsuarioDePrueba() {
		Usuario usuario = new Usuario();
		usuario.setNombre("Usuario API");
		usuario.setEmail("prueba.api@test.local");
		usuario.setPasswordHash("no-aplica");
		usuario.setRol(RolUsuario.CIUDADANO);
		return usuarioRepository.save(usuario);
	}

	private String autorizacion(Usuario usuario) {
		return "Bearer " + servicioTokens.generar(usuario).token();
	}

	private String solicitudValida() {
		return """
				{
				  "descripcion": "Hueco grande frente al parque",
				  "categoriaIa": "HUECO_VIA",
				  "severidadIa": "ALTA",
				  "latitud": 0.0,
				  "longitud": -70.0,
				  "claveImagenHd": "https://almacenamiento.local/reportes/foto.jpg"
				}
				""";
	}

	private String solicitudEn(double latitud, double longitud, String categoria) {
		String categoriaJson = categoria != null ? "\"" + categoria + "\"" : "null";
		return """
				{
				  "categoriaIa": %s,
				  "latitud": %s,
				  "longitud": %s,
				  "claveImagenHd": "https://almacenamiento.local/reportes/foto.jpg"
				}
				""".formatted(categoriaJson, latitud, longitud);
	}

	private MvcResult crearReporte(String autorizacion, double latitud, double longitud) throws Exception {
		return crearReporte(autorizacion, latitud, longitud, "HUECO_VIA");
	}

	private MvcResult crearReporte(String autorizacion, double latitud, double longitud, String categoria)
			throws Exception {
		return mockMvc.perform(post("/api/reportes")
				.header("Authorization", autorizacion)
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitudEn(latitud, longitud, categoria)))
				.andExpect(status().isCreated())
				.andReturn();
	}

	private Number leerCampo(MvcResult resultado, String campo) throws Exception {
		return JsonPath.read(resultado.getResponse().getContentAsString(), campo);
	}

	@Test
	void crearReporteResponde201YGuardaUbicacionEn9377() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		MvcResult creacion = mockMvc.perform(post("/api/reportes")
				.header("Authorization", autorizacion)
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitudValida()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.categoriaIa").value("HUECO_VIA"))
				.andReturn();

		String cuerpo = creacion.getResponse().getContentAsString();
		assertThat(((Number) JsonPath.read(cuerpo, "$.latitud")).doubleValue())
				.isCloseTo(0.0, within(0.000001));
		assertThat(((Number) JsonPath.read(cuerpo, "$.longitud")).doubleValue())
				.isCloseTo(-70.0, within(0.000001));

		Number id = leerCampo(creacion, "$.id");

		entityManager.flush();

		Object srid = entityManager
				.createNativeQuery("SELECT ST_SRID(ubicacion) FROM reportes WHERE id = :id")
				.setParameter("id", id.longValue())
				.getSingleResult();
		assertThat(((Number) srid).intValue()).isEqualTo(9377);
	}

	@Test
	void consultarReporteResponde200() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		MvcResult creacion = mockMvc.perform(post("/api/reportes")
				.header("Authorization", autorizacion)
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitudValida()))
				.andExpect(status().isCreated())
				.andReturn();

		Number id = leerCampo(creacion, "$.id");

		mockMvc.perform(get("/api/reportes/" + id.longValue())
				.header("Authorization", autorizacion))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.usuarioId").value(usuario.getId().intValue()))
				.andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.latitud").isNumber());
	}

	@Test
	void deduplicacionAgrupaReportesDentroDe5MetrosBajoElPadre() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		double saltoCuatroMetros = 0.00004;
		double saltoTreintaMetros = 0.00027;

		MvcResult creacionA = crearReporte(autorizacion, LATITUD_BASE, LONGITUD_BASE);
		Number idA = leerCampo(creacionA, "$.id");
		assertThat(leerCampo(creacionA, "$.reportePadreId")).isNull();

		MvcResult creacionB = crearReporte(autorizacion, LATITUD_BASE, LONGITUD_BASE + saltoCuatroMetros);
		assertThat(leerCampo(creacionB, "$.reportePadreId")).isEqualTo(idA);

		MvcResult creacionC = crearReporte(autorizacion, LATITUD_BASE, LONGITUD_BASE + 2 * saltoCuatroMetros);
		assertThat(leerCampo(creacionC, "$.reportePadreId")).isEqualTo(idA);

		MvcResult creacionD = crearReporte(autorizacion, LATITUD_BASE, LONGITUD_BASE + saltoTreintaMetros);
		assertThat(leerCampo(creacionD, "$.reportePadreId")).isNull();
	}

	@Test
	void deduplicacionSoloAgrupaSiCoincideLaCategoria() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		double saltoCuatroMetros = 0.00004;

		MvcResult creacionA = crearReporte(autorizacion, LATITUD_BASE, LONGITUD_BASE, "HUECO_VIA");
		Number idA = leerCampo(creacionA, "$.id");

		MvcResult creacionB = crearReporte(autorizacion, LATITUD_BASE, LONGITUD_BASE + saltoCuatroMetros,
				"FUGA_AGUA");
		assertThat(leerCampo(creacionB, "$.reportePadreId")).isNull();

		MvcResult creacionC = crearReporte(autorizacion, LATITUD_BASE, LONGITUD_BASE + saltoCuatroMetros,
				"HUECO_VIA");
		assertThat(leerCampo(creacionC, "$.reportePadreId")).isEqualTo(idA);

		MvcResult creacionD = crearReporte(autorizacion, LATITUD_BASE + 0.003, LONGITUD_BASE, null);
		assertThat(leerCampo(creacionD, "$.reportePadreId")).isNull();
		String categoriaDeD = JsonPath.read(creacionD.getResponse().getContentAsString(), "$.categoriaIa");
		assertThat(categoriaDeD).isNotBlank();
	}

	@Test
	void solicitudInvalidaResponde400ConDetalleDeCampos() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		String solicitud = """
				{
				  "claveImagenHd": "",
				  "latitud": null,
				  "longitud": null
				}
				""";

		mockMvc.perform(post("/api/reportes")
				.header("Authorization", autorizacion)
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitud))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Datos invalidos"))
				.andExpect(jsonPath("$.campos.latitud").exists())
				.andExpect(jsonPath("$.campos.longitud").exists())
				.andExpect(jsonPath("$.campos.claveImagenHd").exists());
	}

	@Test
	void categoriaDesconocidaResponde400() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		String solicitud = """
				{
				  "latitud": 3.4516,
				  "longitud": -76.5320,
				  "claveImagenHd": "https://almacenamiento.local/reportes/foto.jpg",
				  "categoriaIa": "NO_EXISTE"
				}
				""";

		mockMvc.perform(post("/api/reportes")
				.header("Authorization", autorizacion)
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitud))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Solicitud malformada"));
	}

	@Test
	void reporteInexistenteResponde404() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		mockMvc.perform(get("/api/reportes/999999")
				.header("Authorization", autorizacion))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("No encontrado"))
				.andExpect(jsonPath("$.mensaje").exists());
	}

	@Test
	void iaCompletaLosCamposFaltantesDeLaClasificacion() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		MvcResult primera = crearReporte(autorizacion, LATITUD_BASE, LONGITUD_BASE, null);
		String categoriaPrimera = JsonPath.read(primera.getResponse().getContentAsString(), "$.categoriaIa");
		String severidadPrimera = JsonPath.read(primera.getResponse().getContentAsString(), "$.severidadIa");
		Double confianzaPrimera = JsonPath.read(primera.getResponse().getContentAsString(), "$.confianzaIa");
		String descripcionPrimera = JsonPath.read(primera.getResponse().getContentAsString(), "$.descripcion");

		assertThat(categoriaPrimera).isNotBlank();
		assertThat(severidadPrimera).isNotBlank();
		assertThat(descripcionPrimera).isNotBlank();
		assertThat(confianzaPrimera).isBetween(0.55, 0.99);

		MvcResult segunda = crearReporte(autorizacion, LATITUD_BASE + 0.003, LONGITUD_BASE, null);
		String categoriaSegunda = JsonPath.read(segunda.getResponse().getContentAsString(), "$.categoriaIa");
		String severidadSegunda = JsonPath.read(segunda.getResponse().getContentAsString(), "$.severidadIa");

		assertThat(categoriaSegunda).isEqualTo(categoriaPrimera);
		assertThat(severidadSegunda).isEqualTo(severidadPrimera);
	}
}
