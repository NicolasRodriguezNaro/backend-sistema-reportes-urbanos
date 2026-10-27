package com.gestion.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SemanticaTest {

	private static final double LAT_LEJOS = 3.4516;
	private static final double LON_LEJOS = -76.5320;

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
		usuario.setNombre("Usuario Semantica");
		usuario.setEmail("prueba.semantica@test.local");
		usuario.setPasswordHash("no-aplica");
		usuario.setRol(RolUsuario.CIUDADANO);
		return usuarioRepository.save(usuario);
	}

	private String autorizacion(Usuario usuario) {
		return "Bearer " + servicioTokens.generar(usuario).token();
	}

	private double[] coordenadasDeUnHospital() {
		Object[] fila = (Object[]) entityManager.createNativeQuery(
				"SELECT ST_Y(ST_Transform(geom, 4326)), ST_X(ST_Transform(geom, 4326))"
						+ " FROM sitios_interes WHERE tipo_sitio = 'HOSPITAL' ORDER BY gid LIMIT 1")
				.getSingleResult();
		return new double[] { ((Number) fila[0]).doubleValue(), ((Number) fila[1]).doubleValue() };
	}

	private MvcResult crearReporte(String autorizacion, double latitud, double longitud, String categoria,
			String severidad) throws Exception {
		String solicitud = """
				{
				  "categoriaIa": "%s",
				  "severidadIa": "%s",
				  "latitud": %s,
				  "longitud": %s,
				  "claveImagenHd": "https://almacenamiento.local/reportes/foto.jpg"
				}
				""".formatted(categoria, severidad, latitud, longitud);

		return mockMvc.perform(post("/api/reportes")
				.header("Authorization", autorizacion)
				.contentType(MediaType.APPLICATION_JSON)
				.content(solicitud))
				.andExpect(status().isCreated())
				.andReturn();
	}

	@Test
	void reporteCercaDeZonaCriticaElevaPrioridadYAsignaEntidad() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		double[] hospital = coordenadasDeUnHospital();
		MvcResult resultado = crearReporte(autorizacion, hospital[0], hospital[1], "FUGA_AGUA", "BAJA");

		String cuerpo = resultado.getResponse().getContentAsString();
		assertThat((String) JsonPath.read(cuerpo, "$.prioridadCalculada")).isEqualTo("ALTA");
		assertThat(((Number) JsonPath.read(cuerpo, "$.entidadAsignadaId")).intValue()).isEqualTo(2);
	}

	@Test
	void reporteCercaDeZonaCriticaConSeveridadAltaEsCritico() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		double[] hospital = coordenadasDeUnHospital();
		MvcResult resultado = crearReporte(autorizacion, hospital[0], hospital[1], "FUGA_AGUA", "ALTA");

		String cuerpo = resultado.getResponse().getContentAsString();
		assertThat((String) JsonPath.read(cuerpo, "$.prioridadCalculada")).isEqualTo("CRITICA");
		assertThat(((Number) JsonPath.read(cuerpo, "$.entidadAsignadaId")).intValue()).isEqualTo(2);
	}

	@Test
	void reporteLejosDeZonaCriticaMantienePrioridadPorSeveridad() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		MvcResult resultado = crearReporte(autorizacion, LAT_LEJOS, LON_LEJOS, "HUECO_VIA", "MEDIA");

		String cuerpo = resultado.getResponse().getContentAsString();
		assertThat((String) JsonPath.read(cuerpo, "$.prioridadCalculada")).isEqualTo("MEDIA");
		assertThat(((Number) JsonPath.read(cuerpo, "$.entidadAsignadaId")).intValue()).isEqualTo(1);
	}
}
