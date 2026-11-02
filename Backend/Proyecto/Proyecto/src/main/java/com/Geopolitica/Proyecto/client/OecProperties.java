package com.Geopolitica.Proyecto.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración de la OEC API: mismo cubo/año que ya usa el frontend (ver Frontend/.env). */
@ConfigurationProperties(prefix = "oec")
public record OecProperties(String baseUrl, String cube, String anio) {
}
