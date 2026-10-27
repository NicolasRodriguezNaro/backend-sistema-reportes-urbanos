package com.gestion.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI apiGestionIncidencias() {
		return new OpenAPI()
				.info(new Info()
						.title("Plataforma Inteligente de Gestion de Incidencias Urbanas")
						.version("1.0.0")
						.description("API para el reporte ciudadano, clasificacion automatica, "
								+ "analisis semantico y gestion de incidencias urbanas"))
				.components(new Components().addSecuritySchemes("bearer-jwt",
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("Token JWT obtenido en /api/auth/login")))
				.addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
	}
}
