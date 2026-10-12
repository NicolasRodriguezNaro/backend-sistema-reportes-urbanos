package com.gestion.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.backend.modelo.EstadoReporte;
import com.gestion.backend.modelo.Reporte;
import com.gestion.backend.modelo.RolUsuario;
import com.gestion.backend.modelo.Usuario;
import com.gestion.backend.repositorio.ReporteRepository;
import com.gestion.backend.repositorio.UsuarioRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class ReporteGeometriaTest {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private ReporteRepository reporteRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void guardaYRecuperaUbicacionConSrid9377() {
		Usuario usuario = new Usuario();
		usuario.setNombre("Usuario de prueba");
		usuario.setEmail("prueba.geometria@test.local");
		usuario.setPasswordHash("no-aplica");
		usuario.setRol(RolUsuario.CIUDADANO);
		usuario = usuarioRepository.save(usuario);

		double longitudWgs84 = -75.6062;
		double latitudWgs84 = 1.6144;

		Object[] transformada = (Object[]) entityManager.createNativeQuery(
				"SELECT ST_X(g), ST_Y(g) FROM (SELECT ST_Transform(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), 9377) AS g) t")
				.setParameter("lon", longitudWgs84)
				.setParameter("lat", latitudWgs84)
				.getSingleResult();

		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 9377);
		Point ubicacion = geometryFactory.createPoint(
				new Coordinate(((Number) transformada[0]).doubleValue(), ((Number) transformada[1]).doubleValue()));

		Reporte reporte = new Reporte();
		reporte.setUsuario(usuario);
		reporte.setUbicacion(ubicacion);
		reporte.setDescripcion("Reporte de prueba de geometria");
		reporte.setUrlImagenHd("https://almacenamiento.local/reportes/foto-prueba.jpg");

		Reporte guardado = reporteRepository.save(reporte);
		entityManager.flush();
		entityManager.clear();

		Reporte recuperado = reporteRepository.findById(guardado.getId()).orElseThrow();

		assertThat(recuperado.getUbicacion()).isNotNull();
		assertThat(recuperado.getUbicacion().getSRID()).isEqualTo(9377);
		assertThat(recuperado.getUbicacion().getX()).isCloseTo(ubicacion.getX(), within(0.000001));
		assertThat(recuperado.getUbicacion().getY()).isCloseTo(ubicacion.getY(), within(0.000001));
		assertThat(recuperado.getEstado()).isEqualTo(EstadoReporte.PENDIENTE);
		assertThat(recuperado.getFechaCreacion()).isNotNull();
		assertThat(recuperado.getFechaActualizacion()).isNotNull();
	}
}
