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
class BarrioTest {

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
		usuario.setNombre("Usuario Barrio");
		usuario.setEmail("prueba.barrio@test.local");
		usuario.setPasswordHash("no-aplica");
		usuario.setRol(RolUsuario.CIUDADANO);
		return usuarioRepository.save(usuario);
	}

	private String autorizacion(Usuario usuario) {
		return "Bearer " + servicioTokens.generar(usuario).token();
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
	void reporteDentroDeUnBarrioQuedaAsignado() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		Object[] fila = (Object[]) entityManager.createNativeQuery(
				"SELECT b.gid, ST_Y(ST_Transform(ST_PointOnSurface(b.geom), 4326)),"
						+ " ST_X(ST_Transform(ST_PointOnSurface(b.geom), 4326))"
						+ " FROM barrios b ORDER BY b.gid LIMIT 1")
				.getSingleResult();

		int gid = ((Number) fila[0]).intValue();
		double latitud = ((Number) fila[1]).doubleValue();
		double longitud = ((Number) fila[2]).doubleValue();

		MvcResult resultado = crearReporte(autorizacion, latitud, longitud, "FALLA_ALUMBRADO", "BAJA");

		int barrioAsignado = ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.barrioId"))
				.intValue();
		assertThat(barrioAsignado).isEqualTo(gid);
	}

	@Test
	void reporteFueraDeCoberturaNoTieneBarrio() throws Exception {
		Usuario usuario = crearUsuarioDePrueba();
		String autorizacion = autorizacion(usuario);

		MvcResult resultado = crearReporte(autorizacion, 3.4516, -76.5320, "HUECO_VIA", "MEDIA");

		Object barrioId = JsonPath.read(resultado.getResponse().getContentAsString(), "$.barrioId");
		assertThat(barrioId).isNull();
	}
}
