package com.gestion.backend.servicio;

import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.backend.almacenamiento.AlmacenamientoArchivos;
import com.gestion.backend.dto.ReporteCreacionRequest;
import com.gestion.backend.dto.ReporteResponse;
import com.gestion.backend.error.RecursoNoEncontradoException;
import com.gestion.backend.geoespacial.ServicioGeoespacial;
import com.gestion.backend.ia.ClasificadorIncidencias;
import com.gestion.backend.ia.ResultadoClasificacion;
import com.gestion.backend.modelo.EstadoReporte;
import com.gestion.backend.modelo.Reporte;
import com.gestion.backend.modelo.Severidad;
import com.gestion.backend.modelo.TipoIncidencia;
import com.gestion.backend.modelo.Usuario;
import com.gestion.backend.repositorio.BarrioRepository;
import com.gestion.backend.repositorio.EntidadRepository;
import com.gestion.backend.repositorio.ReporteRepository;
import com.gestion.backend.semantica.AnalisisSemantico;
import com.gestion.backend.semantica.ServicioSemantico;
import com.gestion.backend.seguridad.ServicioUsuarioActual;

@Service
public class ReporteService {

	private static final double RADIO_DEDUPLICACION_METROS = 5.0;

	private final ReporteRepository reporteRepository;
	private final EntidadRepository entidadRepository;
	private final BarrioRepository barrioRepository;
	private final ServicioGeoespacial servicioGeoespacial;
	private final ClasificadorIncidencias clasificadorIncidencias;
	private final ServicioSemantico servicioSemantico;
	private final AlmacenamientoArchivos almacenamientoArchivos;
	private final ServicioUsuarioActual servicioUsuarioActual;

	public ReporteService(ReporteRepository reporteRepository, EntidadRepository entidadRepository,
			BarrioRepository barrioRepository, ServicioGeoespacial servicioGeoespacial,
			ClasificadorIncidencias clasificadorIncidencias, ServicioSemantico servicioSemantico,
			AlmacenamientoArchivos almacenamientoArchivos, ServicioUsuarioActual servicioUsuarioActual) {
		this.reporteRepository = reporteRepository;
		this.entidadRepository = entidadRepository;
		this.barrioRepository = barrioRepository;
		this.servicioGeoespacial = servicioGeoespacial;
		this.clasificadorIncidencias = clasificadorIncidencias;
		this.servicioSemantico = servicioSemantico;
		this.almacenamientoArchivos = almacenamientoArchivos;
		this.servicioUsuarioActual = servicioUsuarioActual;
	}

	@Transactional
	public ReporteResponse crear(ReporteCreacionRequest solicitud) {
		Usuario usuario = servicioUsuarioActual.actual();

		ResultadoClasificacion clasificacion = null;
		if (solicitud.categoriaIa() == null || solicitud.severidadIa() == null || solicitud.descripcion() == null) {
			clasificacion = clasificadorIncidencias.clasificar(solicitud.claveImagenHd());
		}

		TipoIncidencia categoria = solicitud.categoriaIa() != null ? solicitud.categoriaIa() : clasificacion.categoria();
		Severidad severidad = solicitud.severidadIa() != null ? solicitud.severidadIa() : clasificacion.severidad();
		String descripcion = solicitud.descripcion() != null ? solicitud.descripcion()
				: clasificacion.descripcionBorrador();

		Reporte reporte = new Reporte();
		reporte.setUsuario(usuario);

		Point ubicacion = servicioGeoespacial.a9377(solicitud.latitud(), solicitud.longitud());
		reporte.setUbicacion(ubicacion);

		servicioGeoespacial.buscarBarrio(ubicacion)
				.ifPresent(barrioId -> reporte.setBarrio(barrioRepository.getReferenceById(barrioId)));

		servicioGeoespacial.buscarReportePadre(ubicacion, RADIO_DEDUPLICACION_METROS, categoria)
				.ifPresent(padreId -> reporte.setReportePadre(reporteRepository.getReferenceById(padreId)));

		reporte.setCategoriaIa(categoria);
		reporte.setSeveridadIa(severidad);
		reporte.setDescripcion(descripcion);
		if (clasificacion != null) {
			reporte.setConfianzaIa(clasificacion.confianza());
		}
		reporte.setUrlImagenHd(solicitud.claveImagenHd());
		reporte.setUrlImagenThumb(solicitud.claveImagenThumb());

		Reporte guardado = reporteRepository.save(reporte);

		AnalisisSemantico analisis = servicioSemantico.analizar(guardado);
		guardado.setPrioridadCalculada(analisis.prioridad());
		if (analisis.entidadId() != null) {
			guardado.setEntidadAsignada(entidadRepository.getReferenceById(analisis.entidadId()));
		}

		return aRespuesta(guardado);
	}

	@Transactional(readOnly = true)
	public ReporteResponse obtener(Long id) {
		Reporte reporte = reporteRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Reporte no encontrado con id " + id));
		return aRespuesta(reporte);
	}

	@Transactional
	public ReporteResponse cambiarEstado(Long id, EstadoReporte nuevoEstado) {
		Reporte reporte = reporteRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Reporte no encontrado con id " + id));
		reporte.setEstado(nuevoEstado);
		Reporte guardado = reporteRepository.saveAndFlush(reporte);
		return aRespuesta(guardado);
	}

	private ReporteResponse aRespuesta(Reporte reporte) {
		ServicioGeoespacial.CoordenadasGps gps = servicioGeoespacial.a4326(reporte.getUbicacion());
		String claveImagenHd = reporte.getUrlImagenHd();
		String claveImagenThumb = reporte.getUrlImagenThumb();

		return new ReporteResponse(
				reporte.getId(),
				reporte.getEstado(),
				reporte.getCategoriaIa(),
				reporte.getSeveridadIa(),
				reporte.getPrioridadCalculada(),
				reporte.getConfianzaIa(),
				reporte.getDescripcion(),
				gps.latitud(),
				gps.longitud(),
				claveImagenHd,
				resolverUrl(claveImagenHd),
				claveImagenThumb,
				resolverUrl(claveImagenThumb),
				reporte.getFechaCreacion(),
				reporte.getFechaActualizacion(),
				reporte.getUsuario().getId(),
				reporte.getBarrio() != null ? reporte.getBarrio().getGid() : null,
				reporte.getEntidadAsignada() != null ? reporte.getEntidadAsignada().getId() : null,
				reporte.getReportePadre() != null ? reporte.getReportePadre().getId() : null);
	}

	private String resolverUrl(String valor) {
		if (valor == null) {
			return null;
		}
		if (valor.startsWith("http://") || valor.startsWith("https://")) {
			return valor;
		}
		return almacenamientoArchivos.urlDe(valor);
	}
}
