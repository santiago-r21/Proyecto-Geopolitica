package com.Geopolitica.Proyecto.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente HTTP mínimo para la OEC API (motor Tesseract): 2 reintentos con backoff
 * ante 5xx (la OEC devuelve errores esporádicos con consultas aún no cacheadas en
 * su lado), sin reintento ante 4xx o fallos de red/parseo (esos se propagan de inmediato).
 */
@Component
public class OecClient {

	private static final int REINTENTOS = 2;
	private static final long ESPERA_MS = 1500;

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final OecProperties propiedades;

	public OecClient(OecProperties propiedades, ObjectMapper objectMapper) {
		this.propiedades = propiedades;
		this.objectMapper = objectMapper;
		this.restClient = RestClient.create();
	}

	/**
	 * Ejecuta una consulta `data.jsonrecords` y devuelve las filas del campo `data`.
	 * @param rutaRelativa p.ej. "data.jsonrecords?cube=...&drilldowns=..."
	 */
	public List<JsonNode> fetchRecords(String rutaRelativa) {
		URI uri = URI.create(propiedades.baseUrl() + "/" + rutaRelativa);

		for (int intento = 0; intento <= REINTENTOS; intento++) {
			if (intento > 0) {
				esperar(ESPERA_MS * intento);
			}

			try {
				String cuerpo = restClient.get()
					.uri(uri)
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.body(String.class);
				return parsearData(cuerpo);
			} catch (HttpServerErrorException err) {
				// La OEC devuelve 5xx esporádicos cuando la consulta aún no está en su
				// caché; el mismo request suele funcionar al reintentar.
				if (intento == REINTENTOS) {
					throw new OecException(
						"La OEC API respondió con un error (" + err.getStatusCode().value() + "). Inténtalo más tarde.",
						err.getStatusCode().value(), err);
				}
			} catch (HttpClientErrorException err) {
				throw new OecException(
					"La OEC API respondió con un error (" + err.getStatusCode().value() + ").",
					err.getStatusCode().value(), err);
			} catch (RestClientException err) {
				throw new OecException(
					"No se pudo conectar con la OEC API. Revisa tu conexión e inténtalo de nuevo.", null, err);
			}
		}
		throw new OecException("No se pudo conectar con la OEC API.");
	}

	private List<JsonNode> parsearData(String cuerpo) {
		JsonNode raiz;
		try {
			raiz = objectMapper.readTree(cuerpo);
		} catch (Exception err) {
			throw new OecException("La respuesta de la OEC API no es válida.", null, err);
		}
		JsonNode data = raiz.get("data");
		if (data == null || !data.isArray()) {
			throw new OecException("La OEC API no devolvió datos para la consulta.");
		}
		List<JsonNode> filas = new ArrayList<>();
		data.forEach(filas::add);
		return filas;
	}

	/** Exportaciones del país, desglosadas por país socio (importador). */
	public String exportacionesPorSocio(String oecId, String anio) {
		return ("data.jsonrecords?cube=%s&measures=Trade+Value&drilldowns=Importer+Country"
			+ "&include=Exporter+Country:%s;Year:%s&sort=Trade+Value.desc")
			.formatted(propiedades.cube(), oecId, anio);
	}

	/** Importaciones del país, desglosadas por país socio (exportador). */
	public String importacionesPorSocio(String oecId, String anio) {
		return ("data.jsonrecords?cube=%s&measures=Trade+Value&drilldowns=Exporter+Country"
			+ "&include=Importer+Country:%s;Year:%s&sort=Trade+Value.desc")
			.formatted(propiedades.cube(), oecId, anio);
	}

	private void esperar(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
