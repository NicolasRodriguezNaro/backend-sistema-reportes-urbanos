package com.gestion.backend.servicio;

import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.backend.dto.ReporteCreacionRequest;
import com.gestion.backend.dto.ReporteResponse;
import com.gestion.backend.error.RecursoNoEncontradoException;
import com.gestion.backend.geoespacial.ServicioGeoespacial;
import com.gestion.backend.modelo.Reporte;
import com.gestion.backend.modelo.Usuario;
import com.gestion.backend.repositorio.ReporteRepository;
import com.gestion.backend.repositorio.UsuarioRepository;

@Service
public class ReporteService {

	private static final double RADIO_DEDUPLICACION_METROS = 5.0;

	private final ReporteRepository reporteRepository;
	private final UsuarioRepository usuarioRepository;
	private final ServicioGeoespacial servicioGeoespacial;

	public ReporteService(ReporteRepository reporteRepository, UsuarioRepository usuarioRepository,
			ServicioGeoespacial servicioGeoespacial) {
		this.reporteRepository = reporteRepository;
		this.usuarioRepository = usuarioRepository;
		this.servicioGeoespacial = servicioGeoespacial;
	}

	@Transactional
	public ReporteResponse crear(ReporteCreacionRequest solicitud) {
		Usuario usuario = usuarioRepository.findById(solicitud.usuarioId())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Usuario no encontrado con id " + solicitud.usuarioId()));

		Reporte reporte = new Reporte();
		reporte.setUsuario(usuario);

		Point ubicacion = servicioGeoespacial.a9377(solicitud.latitud(), solicitud.longitud());
		reporte.setUbicacion(ubicacion);

		servicioGeoespacial.buscarReportePadre(ubicacion, RADIO_DEDUPLICACION_METROS)
				.ifPresent(padreId -> reporte.setReportePadre(reporteRepository.getReferenceById(padreId)));

		reporte.setDescripcion(solicitud.descripcion());
		reporte.setCategoriaIa(solicitud.categoriaIa());
		reporte.setSeveridadIa(solicitud.severidadIa());
		reporte.setUrlImagenHd(solicitud.urlImagenHd());
		reporte.setUrlImagenThumb(solicitud.urlImagenThumb());

		Reporte guardado = reporteRepository.save(reporte);

		return aRespuesta(guardado);
	}

	@Transactional(readOnly = true)
	public ReporteResponse obtener(Long id) {
		Reporte reporte = reporteRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Reporte no encontrado con id " + id));
		return aRespuesta(reporte);
	}

	private ReporteResponse aRespuesta(Reporte reporte) {
		ServicioGeoespacial.CoordenadasGps gps = servicioGeoespacial.a4326(reporte.getUbicacion());

		return new ReporteResponse(
				reporte.getId(),
				reporte.getEstado(),
				reporte.getCategoriaIa(),
				reporte.getSeveridadIa(),
				reporte.getConfianzaIa(),
				reporte.getDescripcion(),
				gps.latitud(),
				gps.longitud(),
				reporte.getUrlImagenHd(),
				reporte.getUrlImagenThumb(),
				reporte.getFechaCreacion(),
				reporte.getFechaActualizacion(),
				reporte.getUsuario().getId(),
				reporte.getBarrio() != null ? reporte.getBarrio().getGid() : null,
				reporte.getEntidadAsignada() != null ? reporte.getEntidadAsignada().getId() : null,
				reporte.getReportePadre() != null ? reporte.getReportePadre().getId() : null);
	}
}
