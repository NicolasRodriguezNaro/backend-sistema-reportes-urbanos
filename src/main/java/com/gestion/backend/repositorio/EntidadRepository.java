package com.gestion.backend.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gestion.backend.modelo.Entidad;

public interface EntidadRepository extends JpaRepository<Entidad, Integer> {
}
