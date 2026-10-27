package com.gestion.backend.servicio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestion.backend.almacenamiento.ResolvedorUrlArchivos;
import com.gestion.backend.dto.PaginaResponse;
import com.gestion.backend.dto.ReporteCreacionRequest;
import com.gestion.backend.dto.ReporteResumenResponse;
import com.gestion.backend.dto.ReporteResponse;
import com.gestion.backend.error.RecursoNoEncontradoException;
import com.gestion.backend.geoespacial.ServicioGeoespacial;
import com.gestion.backend.ia.ClasificadorIncidencias;
import com.gestion.backend.ia.ResultadoClasificacion;
import com.gestion.backend.modelo.Entidad;
import com.gestion.backend.modelo.EstadoReporte;
import com.gestion.backend.modelo.PrioridadCalculada;
import com.gestion.backend.modelo.Reporte;
import com.gestion.backend.modelo.Severidad;
import com.gestion.backend.modelo.TipoIncidencia;
import com.gestion.backend.modelo.Usuario;
import com.gestion.backend.repositorio.BarrioRepository;
import com.gestion.backend.repositorio.EntidadRepository;
import com.gestion.backend.repositorio.ReporteEspecificaciones;
import com.gestion.backend.repositorio.ReporteFiltros;
import com.gestion.backend.repositorio.ReporteRepository;
import com.gestion.backend.semantica.AnalisisSemantico;
import com.gestion.backend.semantica.ServicioSemantico;
import com.gestion.backend.seguridad.AlcanceReportes;

@Service
public class ReporteService {

	private static final double RADIO_DEDUPLICACION_METROS = 5.0;

	private final ReporteRepository reporteRepository;
	private final EntidadRepository entidadRepository;
	private final BarrioRepository barrioRepository;
	private final ServicioGeoespacial servicioGeoespacial;
	private final ClasificadorIncidencias clasificadorIncidencias;
	private final ServicioSemantico servicioSemantico;
	private final ResolvedorUrlArchivos resolvedorUrlArchivos;
	private final AlcanceReportes alcanceReportes;

	public ReporteService(ReporteRepository reporteRepository, EntidadRepository entidadRepository,
			BarrioRepository barrioRepository, ServicioGeoespacial servicioGeoespacial,
			ClasificadorIncidencias clasificadorIncidencias, ServicioSemantico servicioSemantico,
			ResolvedorUrlArchivos resolvedorUrlArchivos, AlcanceReportes alcanceReportes) {
		this.reporteRepository = reporteRepository;
		this.entidadRepository = entidadRepository;
		this.barrioRepository = barrioRepository;
		this.servicioGeoespacial = servicioGeoespacial;
		this.clasificadorIncidencias = clasificadorIncidencias;
		this.servicioSemantico = servicioSemantico;
		this.resolvedorUrlArchivos = resolvedorUrlArchivos;
		this.alcanceReportes = alcanceReportes;
	}

	@Transactional
	public ReporteResponse crear(ReporteCreacionRequest solicitud) {
		Usuario usuario = alcanceReportes.usuarioActual();

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
		Reporte reporte = buscar(id);
		alcanceReportes.verificarPuedeVer(alcanceReportes.usuarioActual(), reporte);
		return aRespuesta(reporte);
	}

	@Transactional(readOnly = true)
	public PaginaResponse<ReporteResumenResponse> listar(EstadoReporte estado, TipoIncidencia categoria,
			PrioridadCalculada prioridad, Integer barrioId, Integer entidadId, LocalDate desde, LocalDate hasta,
			String texto, int pagina, int tamano) {
		Usuario usuario = alcanceReportes.usuarioActual();
		Integer entidadFiltro = alcanceReportes.esAdministrador(usuario)
				? entidadId
				: alcanceReportes.entidadIdObligatoria(usuario);

		ReporteFiltros filtros = new ReporteFiltros(estado, categoria, prioridad, barrioId, entidadFiltro,
				inicio(desde), finExclusivo(hasta), texto);

		Page<Reporte> resultado = reporteRepository.findAll(ReporteEspecificaciones.conFiltros(filtros),
				pagina(pagina, tamano));
		return aPagina(resultado);
	}

	@Transactional(readOnly = true)
	public PaginaResponse<ReporteResumenResponse> listarMios(int pagina, int tamano) {
		Usuario usuario = alcanceReportes.usuarioActual();
		Page<Reporte> resultado = reporteRepository.findByUsuarioId(usuario.getId(), pagina(pagina, tamano));
		return aPagina(resultado);
	}

	@Transactional
	public ReporteResponse cambiarEstado(Long id, EstadoReporte nuevoEstado) {
		Reporte reporte = buscar(id);
		alcanceReportes.verificarPuedeGestionar(alcanceReportes.usuarioActual(), reporte);
		reporte.setEstado(nuevoEstado);
		Reporte guardado = reporteRepository.saveAndFlush(reporte);
		return aRespuesta(guardado);
	}

	@Transactional
	public ReporteResponse asignar(Long id, Integer entidadId) {
		alcanceReportes.verificarPuedeAsignar(alcanceReportes.usuarioActual());
		Reporte reporte = buscar(id);
		Entidad entidad = entidadRepository.findById(entidadId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Entidad no encontrada con id " + entidadId));
		reporte.setEntidadAsignada(entidad);
		return aRespuesta(reporteRepository.saveAndFlush(reporte));
	}

	private Reporte buscar(Long id) {
		return reporteRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Reporte no encontrado con id " + id));
	}

	private Pageable pagina(int pagina, int tamano) {
		int paginaSegura = Math.max(pagina, 0);
		int tamanoSeguro = Math.min(Math.max(tamano, 1), 100);
		return PageRequest.of(paginaSegura, tamanoSeguro, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
	}

	private PaginaResponse<ReporteResumenResponse> aPagina(Page<Reporte> resultado) {
		List<ReporteResumenResponse> contenido = resultado.getContent().stream().map(this::aResumen).toList();
		return new PaginaResponse<>(contenido, resultado.getNumber(), resultado.getSize(),
				resultado.getTotalElements(), resultado.getTotalPages());
	}

	private ReporteResumenResponse aResumen(Reporte reporte) {
		String claveThumb = reporte.getUrlImagenThumb();
		return new ReporteResumenResponse(
				reporte.getId(),
				reporte.getEstado(),
				reporte.getCategoriaIa(),
				reporte.getSeveridadIa(),
				reporte.getPrioridadCalculada(),
				reporte.getDescripcion(),
				claveThumb,
				resolvedorUrlArchivos.resolver(claveThumb),
				reporte.getBarrio() != null ? reporte.getBarrio().getGid() : null,
				reporte.getEntidadAsignada() != null ? reporte.getEntidadAsignada().getId() : null,
				reporte.getUsuario().getId(),
				reporte.getReportePadre() != null ? reporte.getReportePadre().getId() : null,
				reporte.getFechaCreacion(),
				reporte.getFechaActualizacion());
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
				resolvedorUrlArchivos.resolver(claveImagenHd),
				claveImagenThumb,
				resolvedorUrlArchivos.resolver(claveImagenThumb),
				reporte.getFechaCreacion(),
				reporte.getFechaActualizacion(),
				reporte.getUsuario().getId(),
				reporte.getBarrio() != null ? reporte.getBarrio().getGid() : null,
				reporte.getEntidadAsignada() != null ? reporte.getEntidadAsignada().getId() : null,
				reporte.getReportePadre() != null ? reporte.getReportePadre().getId() : null);
	}

	private Instant inicio(LocalDate fecha) {
		return fecha == null ? null : fecha.atStartOfDay(ZoneId.systemDefault()).toInstant();
	}

	private Instant finExclusivo(LocalDate fecha) {
		return fecha == null ? null : fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
	}
}
