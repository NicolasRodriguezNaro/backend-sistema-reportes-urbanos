package com.gestion.backend.dto;

public record TokenResponse(String token, String tipo, long expiraEnSegundos, UsuarioResumen usuario) {
}
