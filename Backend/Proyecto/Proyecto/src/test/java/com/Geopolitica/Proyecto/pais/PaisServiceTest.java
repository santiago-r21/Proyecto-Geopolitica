package com.Geopolitica.Proyecto.pais;

import com.Geopolitica.Proyecto.cache.ConsultaCache;
import com.Geopolitica.Proyecto.cache.ConsultaCacheRepository;
import com.Geopolitica.Proyecto.oec.OecClient;
import com.Geopolitica.Proyecto.oec.OecProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaisServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final OecClient oecClient = mock(OecClient.class);
	private final ConsultaCacheRepository cacheRepository = mock(ConsultaCacheRepository.class);
	private final OecProperties propiedades =
		new OecProperties("https://api-v2.oec.world/tesseract", "trade_i_baci_a_22", "2023");

	private final PaisService paisService = new PaisService(oecClient, propiedades, cacheRepository, objectMapper);

	@Test
	void consultaLaOecYGuardaEnCacheCuandoNoHayNadaCacheado() throws Exception {
		when(cacheRepository.findByTipoAndClave("CATALOGO_PAISES", "global")).thenReturn(Optional.empty());

		JsonNode filaColombia = objectMapper.readTree(
			"{\"Exporter Country ID\":\"sacol\",\"Exporter Country\":\"Colombia\",\"Exporter Country ISO 2\":\"co\"}");
		JsonNode filaAngola = objectMapper.readTree(
			"{\"Exporter Country ID\":\"afago\",\"Exporter Country\":\"Angola\",\"Exporter Country ISO 2\":\"ao\"}");
		when(oecClient.fetchRecords(any())).thenReturn(List.of(filaColombia, filaAngola));

		List<Pais> resultado = paisService.obtenerCatalogo();

		assertEquals(List.of(new Pais("afago", "Angola", "ao"), new Pais("sacol", "Colombia", "co")), resultado);
		verify(cacheRepository).save(any(ConsultaCache.class));
	}

	@Test
	void noConsultaLaOecCuandoYaHayUnCatalogoCacheado() {
		String json = "[{\"codigo\":\"sacol\",\"nombre\":\"Colombia\",\"iso2\":\"co\"}]";
		ConsultaCache cacheada = new ConsultaCache("CATALOGO_PAISES", "global", json, Instant.now());
		when(cacheRepository.findByTipoAndClave("CATALOGO_PAISES", "global")).thenReturn(Optional.of(cacheada));

		List<Pais> resultado = paisService.obtenerCatalogo();

		assertEquals(List.of(new Pais("sacol", "Colombia", "co")), resultado);
		verify(oecClient, never()).fetchRecords(any());
	}
}
