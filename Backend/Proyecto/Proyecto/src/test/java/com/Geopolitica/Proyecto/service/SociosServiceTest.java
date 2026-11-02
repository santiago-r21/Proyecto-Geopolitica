package com.Geopolitica.Proyecto.service;

import com.Geopolitica.Proyecto.client.OecClient;
import com.Geopolitica.Proyecto.dto.Pais;
import com.Geopolitica.Proyecto.dto.SocioPais;
import com.Geopolitica.Proyecto.model.ConsultaCache;
import com.Geopolitica.Proyecto.repository.ConsultaCacheRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SociosServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final OecClient oecClient = mock(OecClient.class);
	private final ConsultaCacheRepository cacheRepository = mock(ConsultaCacheRepository.class);
	private final PaisService paisService = mock(PaisService.class);

	private final SociosService sociosService =
		new SociosService(oecClient, cacheRepository, objectMapper, paisService);

	private JsonNode fila(String json) {
		return objectMapper.readTree(json);
	}

	@Test
	void consultaLaOecYGuardaEnCacheCuandoNoHayNadaCacheado() {
		when(cacheRepository.findByTipoAndClave("SOCIOS", "sacol:2023")).thenReturn(Optional.empty());
		when(oecClient.exportacionesPorSocio("sacol", "2023")).thenReturn("ruta-exp");
		when(oecClient.importacionesPorSocio("sacol", "2023")).thenReturn("ruta-imp");
		when(oecClient.fetchRecords("ruta-exp"))
			.thenReturn(List.of(fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":100}")));
		when(oecClient.fetchRecords("ruta-imp"))
			.thenReturn(List.of(fila("{\"Exporter Country ID\":\"nausa\",\"Trade Value\":50}")));
		when(paisService.obtenerCatalogo()).thenReturn(List.of(new Pais("nausa", "Estados Unidos", "us")));

		List<SocioPais> resultado = sociosService.obtener("sacol", "2023");

		assertEquals(1, resultado.size());
		SocioPais socio = resultado.get(0);
		assertEquals("nausa", socio.codigo());
		assertEquals("Estados Unidos", socio.nombre());
		assertEquals(100.0, socio.exportaciones());
		assertEquals(50.0, socio.importaciones());
		assertEquals(150.0, socio.comercioTotal());
		assertEquals(100, socio.cuotaPorcentaje());
		verify(cacheRepository).save(any(ConsultaCache.class));
	}

	@Test
	void noConsultaLaOecNiElCatalogoCuandoYaHaySociosCacheados() {
		String json = "[{\"codigo\":\"nausa\",\"nombre\":\"Estados Unidos\",\"iso2\":\"us\","
			+ "\"exportaciones\":100.0,\"importaciones\":50.0,\"comercioTotal\":150.0,\"cuotaPorcentaje\":100}]";
		when(cacheRepository.findByTipoAndClave("SOCIOS", "sacol:2023"))
			.thenReturn(Optional.of(new ConsultaCache("SOCIOS", "sacol:2023", json, Instant.now())));

		List<SocioPais> resultado = sociosService.obtener("sacol", "2023");

		assertEquals(1, resultado.size());
		assertEquals("nausa", resultado.get(0).codigo());
		verify(oecClient, never()).fetchRecords(any());
		verify(paisService, never()).obtenerCatalogo();
	}

	@Test
	void unSocioSinMatchEnElCatalogoSeOmiteSinRomperElResto() {
		when(cacheRepository.findByTipoAndClave("SOCIOS", "sacol:2023")).thenReturn(Optional.empty());
		when(oecClient.exportacionesPorSocio("sacol", "2023")).thenReturn("ruta-exp");
		when(oecClient.importacionesPorSocio("sacol", "2023")).thenReturn("ruta-imp");
		when(oecClient.fetchRecords("ruta-exp")).thenReturn(List.of(
			fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":100}"),
			fila("{\"Importer Country ID\":\"zzdesconocido\",\"Trade Value\":900}")
		));
		when(oecClient.fetchRecords("ruta-imp")).thenReturn(List.of());
		when(paisService.obtenerCatalogo()).thenReturn(List.of(new Pais("nausa", "Estados Unidos", "us")));

		List<SocioPais> resultado = sociosService.obtener("sacol", "2023");

		assertEquals(1, resultado.size());
		assertEquals("nausa", resultado.get(0).codigo());
		assertTrue(resultado.stream().noneMatch(s -> s.codigo().equals("zzdesconocido")));
	}
}
