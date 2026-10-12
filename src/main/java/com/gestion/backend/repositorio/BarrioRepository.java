package com.gestion.backend.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gestion.backend.modelo.Barrio;

public interface BarrioRepository extends JpaRepository<Barrio, Integer> {
}
