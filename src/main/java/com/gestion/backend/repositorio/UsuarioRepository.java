package com.gestion.backend.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gestion.backend.modelo.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
