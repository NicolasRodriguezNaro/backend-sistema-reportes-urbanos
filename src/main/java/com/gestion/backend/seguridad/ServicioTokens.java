package com.gestion.backend.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.gestion.backend.modelo.Usuario;

@Service
public class ServicioTokens {

	private final JwtEncoder codificador;
	private final long expiracionMinutos;

	public ServicioTokens(JwtEncoder codificador,
			@Value("${gestion.seguridad.jwt.expiracion-minutos:120}") long expiracionMinutos) {
		this.codificador = codificador;
		this.expiracionMinutos = expiracionMinutos;
	}

	public TokenGenerado generar(Usuario usuario) {
		Instant ahora = Instant.now();
		Instant expira = ahora.plus(expiracionMinutos, ChronoUnit.MINUTES);

		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer("gestion-backend")
				.issuedAt(ahora)
				.expiresAt(expira)
				.subject(String.valueOf(usuario.getId()))
				.claim("email", usuario.getEmail())
				.claim("nombre", usuario.getNombre())
				.claim("rol", usuario.getRol().name());

		if (usuario.getEntidad() != null) {
			claims.claim("entidadId", usuario.getEntidad().getId());
		}

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = codificador.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();

		return new TokenGenerado(token, Duration.between(ahora, expira).toSeconds());
	}

	public record TokenGenerado(String token, long expiraEnSegundos) {
	}
}
