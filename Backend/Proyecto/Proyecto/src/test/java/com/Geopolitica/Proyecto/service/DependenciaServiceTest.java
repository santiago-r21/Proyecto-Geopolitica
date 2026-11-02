package com.Geopolitica.Proyecto.service;

import com.Geopolitica.Proyecto.client.OecClient;
import com.Geopolitica.Proyecto.client.OecException;
import com.Geopolitica.Proyecto.client.OecProperties;
import com.Geopolitica.Proyecto.dto.Dependencia;
import com.Geopolitica.Proyecto.dto.Pais;
import com.Geopolitica.Proyecto.model.ConsultaCache;
import com.Geopolitica.Proyecto.repository.ConsultaCacheRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DependenciaServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final OecClient oecClient = mock(OecClient.class);
	private final ConsultaCacheRepository cacheRepository = mock(ConsultaCacheRepository.class);
	private final PaisService paisService = mock(PaisService.class);
	private final OecProperties propiedades =
		new OecProperties("https://api-v2.oec.world/tesseract", "trade_i_baci_a_22", "2023");

	private final DependenciaService dependenciaService = new DependenciaService(
		oecClient, propiedades, cacheRepository, objectMapper, new DependenciaTransformer(), paisService
	);

	private JsonNode fila(String json) {
		return objectMapper.readTree(json);
	}

	@Test
	void consultaLaOecYGuardaEnCacheCuandoNoHayNadaCacheado() {
		when(cacheRepository.findByTipoAndClave("DEPENDENCIA", "sacol:2023")).thenReturn(Optional.empty());
		when(paisService.obtenerCatalogo()).thenReturn(List.of(new Pais("sacol", "Colombia", "co")));

		JsonNode filaSocio = fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":100}");
		JsonNode filaSocioImp = fila("{\"Exporter Country ID\":\"nausa\",\"Trade Value\":100}");
		when(oecClient.fetchRecords(argThat(r -> r != null && !r.contains("Section"))))
			.thenReturn(List.of(filaSocio))
			.thenReturn(List.of(filaSocioImp));
		when(oecClient.fetchRecords(argThat(r -> r != null && r.contains("Section")))).thenReturn(List.of());

		Dependencia resultado = dependenciaService.obtener("sacol", "2023");

		assertEquals("sacol", resultado.codigo());
		assertEquals("Colombia", resultado.nombre());
		assertEquals("co", resultado.iso());
		verify(cacheRepository).save(any(ConsultaCache.class));
	}

	@Test
	void noConsultaLaOecNiElCatalogoCuandoYaHayUnaDependenciaCacheada() {
		Dependencia cacheado = new DependenciaTransformer().transformar(
			List.of(fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":100}")),
			List.of(fila("{\"Exporter Country ID\":\"nausa\",\"Trade Value\":100}")),
			List.of(), List.of(), "sacol", "Colombia", "co", "trade_i_baci_a_22", "2023"
		);
		String json = objectMapper.writeValueAsString(cacheado);
		when(cacheRepository.findByTipoAndClave("DEPENDENCIA", "sacol:2023"))
			.thenReturn(Optional.of(new ConsultaCache("DEPENDENCIA", "sacol:2023", json, Instant.now())));

		Dependencia resultado = dependenciaService.obtener("sacol", "2023");

		assertEquals("sacol", resultado.codigo());
		verify(oecClient, never()).fetchRecords(any());
		verify(paisService, never()).obtenerCatalogo();
	}

	@Test
	void siLasConsultasPorSeccionFallanLasCategoriasQuedanEstimadasPeroElResultadoSeSirve() {
		when(cacheRepository.findByTipoAndClave("DEPENDENCIA", "sacol:2023")).thenReturn(Optional.empty());
		when(paisService.obtenerCatalogo()).thenReturn(List.of(new Pais("sacol", "Colombia", "co")));

		when(oecClient.fetchRecords(argThat(r -> r != null && !r.contains("Section"))))
			.thenReturn(List.of(fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":100}")));
		when(oecClient.fetchRecords(argThat(r -> r != null && r.contains("Section"))))
			.thenThrow(new OecException("La OEC no respondió"));

		Dependencia resultado = dependenciaService.obtener("sacol", "2023");

		assertFalse(resultado.meta().haySecciones());
		verify(cacheRepository).save(any(ConsultaCache.class));
	}

	@Test
	void obtenerComparacionDevuelveUnResultadoPorCadaAnioDisponible() {
		when(cacheRepository.findByTipoAndClave(eq("DEPENDENCIA"), any())).thenReturn(Optional.empty());
		when(paisService.obtenerCatalogo()).thenReturn(List.of(new Pais("sacol", "Colombia", "co")));
		when(oecClient.exportacionesPorSocio(eq("sacol"), any())).thenAnswer(inv -> "exp-" + inv.getArgument(1));
		when(oecClient.importacionesPorSocio(eq("sacol"), any())).thenAnswer(inv -> "imp-" + inv.getArgument(1));
		when(oecClient.fetchRecords(argThat(r -> r != null && !r.contains("Section"))))
			.thenReturn(List.of(fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":100}")));
		when(oecClient.fetchRecords(argThat(r -> r != null && r.contains("Section")))).thenReturn(List.of());

		List<Dependencia> resultado = dependenciaService.obtenerComparacion("sacol");

		assertEquals(3, resultado.size());
		assertEquals(List.of("2022", "2023", "2024"),
			resultado.stream().map(d -> d.meta().anio()).toList());
		verify(cacheRepository, times(3)).save(any(ConsultaCache.class));
	}

	@Test
	void obtenerComparacionOmiteElAnioQueFallaSinRomperLosDemas() {
		when(cacheRepository.findByTipoAndClave(eq("DEPENDENCIA"), any())).thenReturn(Optional.empty());
		when(paisService.obtenerCatalogo()).thenReturn(List.of(new Pais("sacol", "Colombia", "co")));

		// Rutas distintas por año (como la implementación real), para poder hacer
		// fallar solo la de 2023 sin afectar 2022/2024.
		when(oecClient.exportacionesPorSocio(eq("sacol"), any())).thenAnswer(inv -> "exp-" + inv.getArgument(1));
		when(oecClient.importacionesPorSocio(eq("sacol"), any())).thenAnswer(inv -> "imp-" + inv.getArgument(1));

		when(oecClient.fetchRecords("exp-2023")).thenThrow(new OecException("La OEC no respondió"));
		when(oecClient.fetchRecords(argThat(r -> r != null && !"exp-2023".equals(r) && !r.contains("Section"))))
			.thenReturn(List.of(fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":100}")));
		when(oecClient.fetchRecords(argThat(r -> r != null && r.contains("Section")))).thenReturn(List.of());

		List<Dependencia> resultado = dependenciaService.obtenerComparacion("sacol");

		assertEquals(2, resultado.size());
		assertEquals(List.of("2022", "2024"), resultado.stream().map(d -> d.meta().anio()).toList());
	}
}
