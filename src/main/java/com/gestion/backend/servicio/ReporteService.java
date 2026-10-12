package com.gestion.backend.servicio;

import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.backend.dto.ReporteCreacionRequest;
import com.gestion.backend.dto.ReporteResponse;
import com.gestion.backend.error.RecursoNoEncontradoException;
import com.gestion.backend.geoespacial.ServicioGeoespacial;
import com.gestion.backend.ia.ClasificadorIncidencias;
import com.gestion.backend.ia.ResultadoClasificacion;
import com.gestion.backend.modelo.Reporte;
import com.gestion.backend.modelo.Severidad;
import com.gestion.backend.modelo.TipoIncidencia;
import com.gestion.backend.modelo.Usuario;
import com.gestion.backend.repositorio.ReporteRepository;
import com.gestion.backend.repositorio.UsuarioRepository;

@Service
public class ReporteService {

	private static final double RADIO_DEDUPLICACION_METROS = 5.0;

	private final ReporteRepository reporteRepository;
	private final UsuarioRepository usuarioRepository;
	private final ServicioGeoespacial servicioGeoespacial;
	private final ClasificadorIncidencias clasificadorIncidencias;

	public ReporteService(ReporteRepository reporteRepository, UsuarioRepository usuarioRepository,
			ServicioGeoespacial servicioGeoespacial, ClasificadorIncidencias clasificadorIncidencias) {
		this.reporteRepository = reporteRepository;
		this.usuarioRepository = usuarioRepository;
		this.servicioGeoespacial = servicioGeoespacial;
		this.clasificadorIncidencias = clasificadorIncidencias;
	}

	@Transactional
	public ReporteResponse crear(ReporteCreacionRequest solicitud) {
		Usuario usuario = usuarioRepository.findById(solicitud.usuarioId())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Usuario no encontrado con id " + solicitud.usuarioId()));

		ResultadoClasificacion clasificacion = null;
		if (solicitud.categoriaIa() == null || solicitud.severidadIa() == null || solicitud.descripcion() == null) {
			clasificacion = clasificadorIncidencias.clasificar(solicitud.urlImagenHd());
		}

		TipoIncidencia categoria = solicitud.categoriaIa() != null ? solicitud.categoriaIa() : clasificacion.categoria();
		Severidad severidad = solicitud.severidadIa() != null ? solicitud.severidadIa() : clasificacion.severidad();
		String descripcion = solicitud.descripcion() != null ? solicitud.descripcion()
				: clasificacion.descripcionBorrador();

		Reporte reporte = new Reporte();
		reporte.setUsuario(usuario);

		Point ubicacion = servicioGeoespacial.a9377(solicitud.latitud(), solicitud.longitud());
		reporte.setUbicacion(ubicacion);

		servicioGeoespacial.buscarReportePadre(ubicacion, RADIO_DEDUPLICACION_METROS, categoria)
				.ifPresent(padreId -> reporte.setReportePadre(reporteRepository.getReferenceById(padreId)));

		reporte.setCategoriaIa(categoria);
		reporte.setSeveridadIa(severidad);
		reporte.setDescripcion(descripcion);
		if (clasificacion != null) {
			reporte.setConfianzaIa(clasificacion.confianza());
		}
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
