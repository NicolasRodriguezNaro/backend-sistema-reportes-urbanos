package com.gestion.backend.modelo;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "barrios")
public class Barrio {

	@Id
	private Integer gid;

	@Column(length = 100)
	private String nombre;

	private BigDecimal codComuna;

	private BigDecimal codBarrio;
}
