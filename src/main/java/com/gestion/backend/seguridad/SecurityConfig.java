package com.gestion.backend.seguridad;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecretKey jwtSecretKey(@Value("${gestion.seguridad.jwt.secreto}") String secreto) {
		return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKey clave) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
	}

	@Bean
	public JwtDecoder jwtDecoder(SecretKey clave) {
		return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, ManejadorErroresSeguridad manejador)
			throws Exception {
		JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
		convertidor.setJwtGrantedAuthoritiesConverter(jwt -> {
			String rol = jwt.getClaimAsString("rol");
			return rol == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
		});

		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(autorizacion -> autorizacion
						.requestMatchers("/api/auth/registro", "/api/auth/login").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidor))
						.authenticationEntryPoint(manejador)
						.accessDeniedHandler(manejador))
				.exceptionHandling(excepciones -> excepciones
						.authenticationEntryPoint(manejador)
						.accessDeniedHandler(manejador));

		return http.build();
	}
}
