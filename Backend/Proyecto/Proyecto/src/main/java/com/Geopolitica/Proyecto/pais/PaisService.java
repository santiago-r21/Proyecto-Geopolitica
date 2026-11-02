package com.Geopolitica.Proyecto.pais;

import com.Geopolitica.Proyecto.cache.ConsultaCache;
import com.Geopolitica.Proyecto.cache.ConsultaCacheRepository;
import com.Geopolitica.Proyecto.oec.OecClient;
import com.Geopolitica.Proyecto.oec.OecProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Catálogo completo de países disponibles en la OEC (226 a la fecha), cacheado
 * de forma durable en Postgres: una vez consultado por cualquier usuario, no se
 * vuelve a pedir a la OEC.
 */
@Service
public class PaisService {

	private static final String TIPO_CACHE = "CATALOGO_PAISES";
	private static final String CLAVE_CACHE = "global";

	private final OecClient oecClient;
	private final OecProperties propiedades;
	private final ConsultaCacheRepository cacheRepository;
	private final ObjectMapper objectMapper;

	public PaisService(
		OecClient oecClient,
		OecProperties propiedades,
		ConsultaCacheRepository cacheRepository,
		ObjectMapper objectMapper
	) {
		this.oecClient = oecClient;
		this.propiedades = propiedades;
		this.cacheRepository = cacheRepository;
		this.objectMapper = objectMapper;
	}

	public List<Pais> obtenerCatalogo() {
		return cacheRepository.findByTipoAndClave(TIPO_CACHE, CLAVE_CACHE)
			.map(this::deserializar)
			.orElseGet(this::consultarYCachear);
	}

	private List<Pais> consultarYCachear() {
		String ruta = ("data.jsonrecords?cube=%s&drilldowns=Exporter+Country&measures=Trade+Value"
			+ "&properties=Exporter+Country+ISO+2&include=Year:%s&locale=es")
			.formatted(propiedades.cube(), propiedades.anio());

		List<Pais> paises = oecClient.fetchRecords(ruta).stream()
			.map(this::mapearFila)
			.sorted(Comparator.comparing(Pais::nombre))
			.toList();

		cacheRepository.save(new ConsultaCache(TIPO_CACHE, CLAVE_CACHE, serializar(paises), Instant.now()));
		return paises;
	}

	private Pais mapearFila(JsonNode fila) {
		return new Pais(
			fila.path("Exporter Country ID").asString(),
			fila.path("Exporter Country").asString(),
			fila.path("Exporter Country ISO 2").asString()
		);
	}

	private String serializar(List<Pais> paises) {
		try {
			return objectMapper.writeValueAsString(paises);
		} catch (Exception err) {
			throw new IllegalStateException("No se pudo serializar el catálogo de países.", err);
		}
	}

	private List<Pais> deserializar(ConsultaCache cache) {
		try {
			Pais[] paises = objectMapper.readValue(cache.getResultado(), Pais[].class);
			return new ArrayList<>(List.of(paises));
		} catch (Exception err) {
			throw new IllegalStateException("No se pudo leer el catálogo de países cacheado.", err);
		}
	}
}
