package com.Geopolitica.Proyecto.service;

import com.Geopolitica.Proyecto.client.OecClient;
import com.Geopolitica.Proyecto.client.OecProperties;
import com.Geopolitica.Proyecto.dto.Dependencia;
import com.Geopolitica.Proyecto.dto.Pais;
import com.Geopolitica.Proyecto.model.ConsultaCache;
import com.Geopolitica.Proyecto.repository.ConsultaCacheRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

/**
 * Resumen de dependencia económica/estratégica de un país frente a EE.UU./China/UE,
 * cacheado de forma durable en Postgres por (país, año): una vez consultado por
 * cualquier usuario, no se vuelve a pedir a la OEC (2022-2024 es historia cerrada).
 */
@Service
public class DependenciaService {

	private static final String TIPO_CACHE = "DEPENDENCIA";

	private final OecClient oecClient;
	private final OecProperties propiedades;
	private final ConsultaCacheRepository cacheRepository;
	private final ObjectMapper objectMapper;
	private final DependenciaTransformer transformer;
	private final PaisService paisService;

	public DependenciaService(
		OecClient oecClient,
		OecProperties propiedades,
		ConsultaCacheRepository cacheRepository,
		ObjectMapper objectMapper,
		DependenciaTransformer transformer,
		PaisService paisService
	) {
		this.oecClient = oecClient;
		this.propiedades = propiedades;
		this.cacheRepository = cacheRepository;
		this.objectMapper = objectMapper;
		this.transformer = transformer;
		this.paisService = paisService;
	}

	/** @param codigo id OEC del país (ej. "sacol"), el mismo que devuelve PaisService.obtenerCatalogo() */
	public Dependencia obtener(String codigo, String anio) {
		String clave = clave(codigo, anio);
		return cacheRepository.findByTipoAndClave(TIPO_CACHE, clave)
			.map(this::deserializar)
			.orElseGet(() -> consultarYCachear(codigo, anio));
	}

	private Dependencia consultarYCachear(String codigo, String anio) {
		Pais pais = buscarPais(codigo);

		// Consultas obligatorias: potencias por exportación e importación.
		List<JsonNode> filasExp = oecClient.fetchRecords(oecClient.exportacionesPorSocio(codigo, anio));
		List<JsonNode> filasImp = oecClient.fetchRecords(oecClient.importacionesPorSocio(codigo, anio));

		// Consultas best-effort: desglose por sección HS (para categorías reales).
		// Si fallan, el transformer cae a estimaciones derivadas del índice.
		List<JsonNode> filasExpSeccion = intentarFetch(exportacionesPorSeccion(codigo, anio));
		List<JsonNode> filasImpSeccion = intentarFetch(importacionesPorSeccion(codigo, anio));

		Dependencia dependencia = transformer.transformar(
			filasExp, filasImp, filasExpSeccion, filasImpSeccion,
			codigo, pais.nombre(), pais.iso2(), propiedades.cube(), anio
		);

		cacheRepository.save(new ConsultaCache(TIPO_CACHE, clave(codigo, anio), serializar(dependencia), Instant.now()));
		return dependencia;
	}

	private Pais buscarPais(String codigo) {
		return paisService.obtenerCatalogo().stream()
			.filter(p -> p.codigo().equals(codigo))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("País no soportado: " + codigo));
	}

	private List<JsonNode> intentarFetch(String ruta) {
		try {
			return oecClient.fetchRecords(ruta);
		} catch (RuntimeException err) {
			return List.of();
		}
	}

	private String clave(String codigo, String anio) {
		return codigo + ":" + anio;
	}

	/** 3. Exportaciones del país por Sección HS × país socio. */
	private String exportacionesPorSeccion(String oecId, String anio) {
		return ("data.jsonrecords?cube=%s&measures=Trade+Value&drilldowns=Section,Importer+Country"
			+ "&include=Exporter+Country:%s;Year:%s")
			.formatted(propiedades.cube(), oecId, anio);
	}

	/** 4. Importaciones del país por Sección HS × país socio. */
	private String importacionesPorSeccion(String oecId, String anio) {
		return ("data.jsonrecords?cube=%s&measures=Trade+Value&drilldowns=Section,Exporter+Country"
			+ "&include=Importer+Country:%s;Year:%s")
			.formatted(propiedades.cube(), oecId, anio);
	}

	private String serializar(Dependencia dependencia) {
		try {
			return objectMapper.writeValueAsString(dependencia);
		} catch (Exception err) {
			throw new IllegalStateException("No se pudo serializar el resumen de dependencia.", err);
		}
	}

	private Dependencia deserializar(ConsultaCache cache) {
		try {
			return objectMapper.readValue(cache.getResultado(), Dependencia.class);
		} catch (Exception err) {
			throw new IllegalStateException("No se pudo leer el resumen de dependencia cacheado.", err);
		}
	}
}
