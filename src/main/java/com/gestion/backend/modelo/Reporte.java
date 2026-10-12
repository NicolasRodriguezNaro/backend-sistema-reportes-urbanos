package com.gestion.backend.modelo;

import java.time.Instant;

import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reportes")
public class Reporte {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "barrio_id")
	private Barrio barrio;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entidad_asignada_id")
	private Entidad entidadAsignada;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporte_padre_id")
	private Reporte reportePadre;

	@Column(name = "ubicacion", nullable = false, columnDefinition = "geometry(Point, 9377)")
	private Point ubicacion;

	@Enumerated(EnumType.STRING)
	@Column(length = 100)
	private TipoIncidencia categoriaIa;

	@Enumerated(EnumType.STRING)
	@Column(length = 50)
	private Severidad severidadIa;

	private Double confianzaIa;

	@Column(columnDefinition = "TEXT")
	private String descripcion;

	@Column(nullable = false, length = 500)
	private String urlImagenHd;

	@Column(length = 500)
	private String urlImagenThumb;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private EstadoReporte estado;

	@Enumerated(EnumType.STRING)
	@Column(length = 50)
	private PrioridadCalculada prioridadCalculada;

	private Instant fechaCreacion;

	private Instant fechaActualizacion;

	@PrePersist
	void antesDeInsertar() {
		Instant ahora = Instant.now();
		if (fechaCreacion == null) {
			fechaCreacion = ahora;
		}
		fechaActualizacion = ahora;
		if (estado == null) {
			estado = EstadoReporte.PENDIENTE;
		}
	}

	@PreUpdate
	void antesDeActualizar() {
		fechaActualizacion = Instant.now();
	}
}
