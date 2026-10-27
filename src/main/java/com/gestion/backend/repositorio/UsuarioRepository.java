package com.gestion.backend.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gestion.backend.modelo.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);
}
