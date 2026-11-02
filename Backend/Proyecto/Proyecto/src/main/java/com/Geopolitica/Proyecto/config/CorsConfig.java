package com.Geopolitica.Proyecto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Permite que el frontend (Vite, en otro origen) llame a la API sin bloqueo de CORS. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	// Patrones, no orígenes exactos: Vite cambia de puerto si el suyo está ocupado
	// (5173, 5174, 5175...), así que fijar un único puerto es frágil en desarrollo.
	@Value("${app.cors.allowed-origin-patterns}")
	private String[] allowedOriginPatterns;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOriginPatterns(allowedOriginPatterns)
			.allowedMethods("GET");
	}
}
