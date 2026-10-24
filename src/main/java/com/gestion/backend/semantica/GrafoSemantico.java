package com.gestion.backend.semantica;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gestion.backend.modelo.PrioridadCalculada;
import com.gestion.backend.modelo.Severidad;
import com.gestion.backend.modelo.TipoEntidad;
import com.gestion.backend.modelo.TipoIncidencia;

import jakarta.annotation.PreDestroy;

@Component
public class GrafoSemantico {

	private static final Logger log = LoggerFactory.getLogger(GrafoSemantico.class);

	private static final String NS = "http://infraestructura.org/ontologia#";
	private static final String GRAFO_ONTOLOGIA = "urn:gestion:ontologia";
	private static final String GRAFO_REFERENCIA = "urn:gestion:referencia";

	private static final Resource C_SITIO_INTERES = clase("SitioInteres");
	private static final Resource C_ENTIDAD_RESPONSABLE = clase("EntidadResponsable");
	private static final Resource C_HOSPITAL = clase("Hospital");
	private static final Resource C_COLEGIO = clase("Colegio");
	private static final Resource C_UNIVERSIDAD = clase("Universidad");
	private static final Resource C_VIA_PRINCIPAL = clase("ViaPrincipal");
	private static final Resource C_HUECO_VIA = clase("HuecoVia");
	private static final Resource C_FUGA_AGUA = clase("FugaAgua");
	private static final Resource C_FALLA_ALUMBRADO = clase("FallaAlumbrado");
	private static final Resource C_ACUMULACION_BASURA = clase("AcumulacionBasura");
	private static final Resource C_SECRETARIA_OBRAS = clase("SecretariaObrasPublicas");
	private static final Resource C_EMPRESA_SERVICIOS = clase("EmpresaServiciosPublicos");
	private static final Resource C_EMPRESA_ASEO = clase("EmpresaAseo");

	private static final Property P_REPORTE_ID = propiedad("reporteId");
	private static final Property P_SITIO_ID = propiedad("sitioId");
	private static final Property P_ENTIDAD_ID = propiedad("entidadId");
	private static final Property P_SEVERIDAD_IA = propiedad("severidadIA");
	private static final Property P_CONFIANZA_IA = propiedad("confianzaIA");
	private static final Property P_PRIORIDAD_CALCULADA = propiedad("prioridadCalculada");
	private static final Property P_DISTANCIA_METROS = propiedad("distanciaMetros");
	private static final Property P_CERCANO_A = propiedad("cercanoA");
	private static final Property P_ASIGNADO_A = propiedad("asignadoA");

	private final Dataset dataset;
	private final Reasoner razonador;

	public GrafoSemantico(@Value("${gestion.semantica.ruta-tdb:data/jena}") String rutaTdb) {
		try {
			Files.createDirectories(Path.of(rutaTdb));
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo crear la carpeta del grafo semantico: " + rutaTdb, e);
		}
		this.dataset = TDB2Factory.connectDataset(rutaTdb);
		this.razonador = construirRazonador();
		cargarOntologia();
	}

	public record EntidadRef(int id, String nombre, TipoEntidad tipo) {
	}

	public record SitioRef(int gid, String nombre, String tipoSitio) {
	}

	public void sincronizarEstaticos(List<EntidadRef> entidades, List<SitioRef> sitios) {
		dataset.begin(ReadWrite.WRITE);
		try {
			Model referencia = dataset.getNamedModel(GRAFO_REFERENCIA);
			referencia.removeAll();

			for (EntidadRef entidad : entidades) {
				Resource recurso = recurso("Entidad_" + entidad.id());
				referencia.add(recurso, RDF.type, claseDeEntidad(entidad.tipo()));
				referencia.add(recurso, RDF.type, C_ENTIDAD_RESPONSABLE);
				referencia.add(recurso, RDFS.label, entidad.nombre());
				referencia.add(recurso, P_ENTIDAD_ID,
						referencia.createTypedLiteral(String.valueOf(entidad.id()), XSDDatatype.XSDinteger));
			}

			for (SitioRef sitio : sitios) {
				Resource recurso = recurso("Sitio_" + sitio.gid());
				referencia.add(recurso, RDF.type, claseDeSitio(sitio.tipoSitio()));
				referencia.add(recurso, RDF.type, C_SITIO_INTERES);
				referencia.add(recurso, RDFS.label, sitio.nombre());
				referencia.add(recurso, P_SITIO_ID,
						referencia.createTypedLiteral(String.valueOf(sitio.gid()), XSDDatatype.XSDinteger));
			}

			dataset.commit();
		} catch (RuntimeException e) {
			dataset.abort();
			throw e;
		} finally {
			dataset.end();
		}
	}

	public void escribirReporte(Long reporteId, TipoIncidencia categoria, Severidad severidad, Double confianza,
			SitioCercano sitio) {
		dataset.begin(ReadWrite.WRITE);
		try {
			Model dinamico = dataset.getDefaultModel();
			Resource reporte = recurso("Reporte_" + reporteId);
			dinamico.removeAll(reporte, null, null);

			dinamico.add(reporte, RDF.type, claseDeIncidencia(categoria));
			dinamico.add(reporte, P_REPORTE_ID,
					dinamico.createTypedLiteral(String.valueOf(reporteId), XSDDatatype.XSDinteger));

			if (severidad != null) {
				dinamico.add(reporte, P_SEVERIDAD_IA, severidad.name());
			}
			if (confianza != null) {
				dinamico.add(reporte, P_CONFIANZA_IA, dinamico.createTypedLiteral(confianza));
			}

			if (sitio != null) {
				Resource sitioRecurso = recurso("Sitio_" + sitio.gid());
				dinamico.add(sitioRecurso, RDF.type, claseDeSitio(sitio.tipoSitio()));
				dinamico.add(sitioRecurso, RDF.type, C_SITIO_INTERES);
				dinamico.add(sitioRecurso, RDFS.label, sitio.nombre());
				dinamico.add(sitioRecurso, P_SITIO_ID,
						dinamico.createTypedLiteral(String.valueOf(sitio.gid()), XSDDatatype.XSDinteger));

				dinamico.add(reporte, P_CERCANO_A, sitioRecurso);
				dinamico.add(reporte, P_DISTANCIA_METROS, dinamico.createTypedLiteral(sitio.distanciaMetros()));
			}

			dataset.commit();
		} catch (RuntimeException e) {
			dataset.abort();
			throw e;
		} finally {
			dataset.end();
		}
	}

	public InferenciasGrafo inferir(Long reporteId) {
		Model memoria = ModelFactory.createDefaultModel();
		dataset.begin(ReadWrite.READ);
		try {
			memoria.add(dataset.getNamedModel(GRAFO_ONTOLOGIA));
			memoria.add(dataset.getNamedModel(GRAFO_REFERENCIA));
			memoria.add(dataset.getDefaultModel());
		} finally {
			dataset.end();
		}

		InfModel inferido = ModelFactory.createInfModel(razonador, memoria);
		Resource reporte = recurso("Reporte_" + reporteId);

		Set<String> prioridades = new HashSet<>();
		StmtIterator iteradorPrioridades = inferido.listStatements(reporte, P_PRIORIDAD_CALCULADA, (RDFNode) null);
		while (iteradorPrioridades.hasNext()) {
			Statement sentencia = iteradorPrioridades.next();
			if (sentencia.getObject().isLiteral()) {
				prioridades.add(sentencia.getLiteral().getString());
			}
		}
		iteradorPrioridades.close();

		Integer entidadId = null;
		StmtIterator iteradorEntidad = inferido.listStatements(reporte, P_ASIGNADO_A, (RDFNode) null);
		if (iteradorEntidad.hasNext()) {
			Resource entidad = iteradorEntidad.next().getObject().asResource();
			Statement id = entidad.getProperty(P_ENTIDAD_ID);
			if (id != null && id.getObject().isLiteral()) {
				entidadId = id.getLiteral().getInt();
			}
		}
		iteradorEntidad.close();

		return new InferenciasGrafo(prioridades, Optional.ofNullable(entidadId));
	}

	public void materializar(Long reporteId, PrioridadCalculada prioridad, Integer entidadId) {
		dataset.begin(ReadWrite.WRITE);
		try {
			Model dinamico = dataset.getDefaultModel();
			Resource reporte = recurso("Reporte_" + reporteId);

			dinamico.removeAll(reporte, P_PRIORIDAD_CALCULADA, null);
			if (prioridad != null) {
				dinamico.add(reporte, P_PRIORIDAD_CALCULADA, prioridad.name());
			}

			dinamico.removeAll(reporte, P_ASIGNADO_A, null);
			if (entidadId != null) {
				dinamico.add(reporte, P_ASIGNADO_A, recurso("Entidad_" + entidadId));
			}

			dataset.commit();
		} catch (RuntimeException e) {
			dataset.abort();
			throw e;
		} finally {
			dataset.end();
		}
	}

	@PreDestroy
	public void cerrar() {
		dataset.close();
	}

	private void cargarOntologia() {
		dataset.begin(ReadWrite.WRITE);
		try {
			Model ontologia = dataset.getNamedModel(GRAFO_ONTOLOGIA);
			ontologia.removeAll();
			try (InputStream entrada = getClass().getResourceAsStream("/semantica/ontologia.ttl")) {
				if (entrada == null) {
					throw new IllegalStateException("No se encontro /semantica/ontologia.ttl");
				}
				RDFDataMgr.read(ontologia, entrada, Lang.TURTLE);
			}
			dataset.commit();
		} catch (Exception e) {
			dataset.abort();
			throw new IllegalStateException("No se pudo cargar la ontologia semantica", e);
		} finally {
			dataset.end();
		}
	}

	private Reasoner construirRazonador() {
		try (InputStream entrada = getClass().getResourceAsStream("/semantica/reglas.rules")) {
			if (entrada == null) {
				throw new IllegalStateException("No se encontro /semantica/reglas.rules");
			}
			String texto = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
			List<Rule> reglas = Rule.parseRules(texto);
			log.info("Grafo semantico: {} reglas cargadas", reglas.size());
			return new GenericRuleReasoner(reglas);
		} catch (IOException e) {
			throw new IllegalStateException("No se pudieron cargar las reglas semanticas", e);
		}
	}

	private Resource claseDeIncidencia(TipoIncidencia tipo) {
		return switch (tipo) {
			case HUECO_VIA -> C_HUECO_VIA;
			case FUGA_AGUA -> C_FUGA_AGUA;
			case FALLA_ALUMBRADO -> C_FALLA_ALUMBRADO;
			case ACUMULACION_BASURA -> C_ACUMULACION_BASURA;
		};
	}

	private Resource claseDeSitio(String tipoSitio) {
		return switch (tipoSitio) {
			case "HOSPITAL" -> C_HOSPITAL;
			case "INSTITUCION_EDUCATIVA", "COLEGIO" -> C_COLEGIO;
			case "UNIVERSIDAD" -> C_UNIVERSIDAD;
			case "VIA_ARTERIA", "VIA_PRINCIPAL" -> C_VIA_PRINCIPAL;
			default -> C_SITIO_INTERES;
		};
	}

	private Resource claseDeEntidad(TipoEntidad tipo) {
		return switch (tipo) {
			case SECRETARIA_OBRAS_PUBLICAS -> C_SECRETARIA_OBRAS;
			case EMPRESA_SERVICIOS_PUBLICOS -> C_EMPRESA_SERVICIOS;
			case EMPRESA_ASEO -> C_EMPRESA_ASEO;
		};
	}

	private static Resource clase(String nombreLocal) {
		return ResourceFactory.createResource(NS + nombreLocal);
	}

	private static Property propiedad(String nombreLocal) {
		return ResourceFactory.createProperty(NS + nombreLocal);
	}

	private static Resource recurso(String nombreLocal) {
		return ResourceFactory.createResource(NS + nombreLocal);
	}
}
