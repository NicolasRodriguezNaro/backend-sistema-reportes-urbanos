package com.gestion.backend.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gestion.backend.modelo.Reporte;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {
}
