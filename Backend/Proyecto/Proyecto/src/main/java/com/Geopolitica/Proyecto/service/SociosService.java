package com.Geopolitica.Proyecto.service;

import com.Geopolitica.Proyecto.client.OecClient;
import com.Geopolitica.Proyecto.dto.Pais;
import com.Geopolitica.Proyecto.dto.SocioPais;
import com.Geopolitica.Proyecto.model.ConsultaCache;
import com.Geopolitica.Proyecto.repository.ConsultaCacheRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comercio de un país con cada socio real (sin agrupar en los 4 bloques del
 * resumen de dependencia), para el mapa interactivo. Cacheado de forma durable
 * en Postgres por (país, año), igual que DependenciaService.
 */
@Service
public class SociosService {

	private static final String TIPO_CACHE = "SOCIOS";

	private final OecClient oecClient;
	private final ConsultaCacheRepository cacheRepository;
	private final ObjectMapper objectMapper;
	private final PaisService paisService;

	public SociosService(
		OecClient oecClient,
		ConsultaCacheRepository cacheRepository,
		ObjectMapper objectMapper,
		PaisService paisService
	) {
		this.oecClient = oecClient;
		this.cacheRepository = cacheRepository;
		this.objectMapper = objectMapper;
		this.paisService = paisService;
	}

	/** @param codigo id OEC del país (ej. "sacol"), el mismo que devuelve GET /api/paises */
	public List<SocioPais> obtener(String codigo, String anio) {
		String clave = clave(codigo, anio);
		return cacheRepository.findByTipoAndClave(TIPO_CACHE, clave)
			.map(this::deserializar)
			.orElseGet(() -> consultarYCachear(codigo, anio));
	}

	private List<SocioPais> consultarYCachear(String codigo, String anio) {
		List<JsonNode> filasExp = oecClient.fetchRecords(oecClient.exportacionesPorSocio(codigo, anio));
		List<JsonNode> filasImp = oecClient.fetchRecords(oecClient.importacionesPorSocio(codigo, anio));

		Map<String, double[]> comercioPorSocio = new HashMap<>();
		acumular(comercioPorSocio, filasExp, "Importer Country ID", 0);
		acumular(comercioPorSocio, filasImp, "Exporter Country ID", 1);

		Map<String, Pais> catalogoPorCodigo = new HashMap<>();
		for (Pais pais : paisService.obtenerCatalogo()) {
			catalogoPorCodigo.put(pais.codigo(), pais);
		}

		double totalComercio = comercioPorSocio.values().stream()
			.mapToDouble(valores -> valores[0] + valores[1])
			.sum();

		List<SocioPais> socios = comercioPorSocio.entrySet().stream()
			.filter(entrada -> catalogoPorCodigo.containsKey(entrada.getKey()))
			.map(entrada -> aSocioPais(catalogoPorCodigo.get(entrada.getKey()), entrada.getValue(), totalComercio))
			.sorted(Comparator.comparingDouble(SocioPais::comercioTotal).reversed())
			.toList();

		cacheRepository.save(new ConsultaCache(TIPO_CACHE, clave(codigo, anio), serializar(socios), Instant.now()));
		return socios;
	}

	private void acumular(Map<String, double[]> acc, List<JsonNode> filas, String campoId, int indice) {
		for (JsonNode fila : filas) {
			String codigoSocio = fila.path(campoId).asString();
			double valor = fila.path("Trade Value").asDouble(0);
			double[] valores = acc.computeIfAbsent(codigoSocio, ignorado -> new double[2]);
			valores[indice] += valor;
		}
	}

	private SocioPais aSocioPais(Pais pais, double[] valores, double totalComercio) {
		double exportaciones = valores[0];
		double importaciones = valores[1];
		double comercioTotal = exportaciones + importaciones;
		int cuotaPorcentaje = totalComercio > 0
			? Math.round((float) (comercioTotal / totalComercio * 100))
			: 0;

		return new SocioPais(pais.codigo(), pais.nombre(), pais.iso2(), exportaciones, importaciones, comercioTotal, cuotaPorcentaje);
	}

	private String clave(String codigo, String anio) {
		return codigo + ":" + anio;
	}

	private String serializar(List<SocioPais> socios) {
		try {
			return objectMapper.writeValueAsString(socios);
		} catch (Exception err) {
			throw new IllegalStateException("No se pudo serializar los socios del país.", err);
		}
	}

	private List<SocioPais> deserializar(ConsultaCache cache) {
		try {
			SocioPais[] socios = objectMapper.readValue(cache.getResultado(), SocioPais[].class);
			return new ArrayList<>(List.of(socios));
		} catch (Exception err) {
			throw new IllegalStateException("No se pudo leer los socios cacheados del país.", err);
		}
	}
}
