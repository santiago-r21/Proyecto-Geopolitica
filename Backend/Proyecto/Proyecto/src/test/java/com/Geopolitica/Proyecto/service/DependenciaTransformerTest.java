package com.Geopolitica.Proyecto.service;

import com.Geopolitica.Proyecto.dto.Categoria;
import com.Geopolitica.Proyecto.dto.Dependencia;
import com.Geopolitica.Proyecto.dto.Socio;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependenciaTransformerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final DependenciaTransformer transformer = new DependenciaTransformer();

	private JsonNode fila(String json) {
		return objectMapper.readTree(json);
	}

	@Test
	void calculaIndiceYPorcentajesConFilasConocidas() {
		List<JsonNode> filasExp = List.of(
			fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":500}"), // eeuu
			fila("{\"Importer Country ID\":\"aschn\",\"Trade Value\":300}"), // china
			fila("{\"Importer Country ID\":\"eudeu\",\"Trade Value\":100}"), // ue
			fila("{\"Importer Country ID\":\"saper\",\"Trade Value\":100}")  // otros
		);
		List<JsonNode> filasImp = List.of(
			fila("{\"Exporter Country ID\":\"nausa\",\"Trade Value\":200}"),
			fila("{\"Exporter Country ID\":\"aschn\",\"Trade Value\":200}"),
			fila("{\"Exporter Country ID\":\"eudeu\",\"Trade Value\":400}"),
			fila("{\"Exporter Country ID\":\"saarg\",\"Trade Value\":200}")
		);

		Dependencia resultado = transformer.transformar(
			filasExp, filasImp, List.of(), List.of(),
			"sacol", "Colombia", "co", "trade_i_baci_a_22", "2023"
		);

		// combinado: eeuu=700, china=500, ue=500, otros=300 (total 2000) -> 35/25/25/15, suma exacta
		assertEquals(100, resultado.socios().stream().mapToInt(Socio::porcentaje).sum());
		assertEquals(35, buscarSocio(resultado.socios(), "eeuu").porcentaje());
		assertEquals(25, buscarSocio(resultado.socios(), "china").porcentaje());
		assertEquals(25, buscarSocio(resultado.socios(), "ue").porcentaje());
		assertEquals(15, buscarSocio(resultado.socios(), "otros").porcentaje());

		// orden de presentación: potencias desc, "otros" al final
		assertEquals(List.of("eeuu", "china", "ue", "otros"), resultado.socios().stream().map(Socio::clave).toList());

		assertEquals(57, resultado.indiceDependencia());
		assertEquals("Vulnerabilidad Media", resultado.vulnerabilidad().etiqueta());
	}

	@Test
	void elMetodoDelMayorRestoRepartaExactamente100ConRestosEmpatados() {
		// 3 bloques en tercios exactos (33.33...%) + otros en 0 -> floor da 99, hay que repartir 1.
		List<JsonNode> filasExp = List.of(
			fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":100}"),
			fila("{\"Importer Country ID\":\"aschn\",\"Trade Value\":100}"),
			fila("{\"Importer Country ID\":\"eudeu\",\"Trade Value\":100}")
		);
		List<JsonNode> filasImp = List.of(
			fila("{\"Exporter Country ID\":\"nausa\",\"Trade Value\":100}"),
			fila("{\"Exporter Country ID\":\"aschn\",\"Trade Value\":100}"),
			fila("{\"Exporter Country ID\":\"eudeu\",\"Trade Value\":100}")
		);

		Dependencia resultado = transformer.transformar(
			filasExp, filasImp, List.of(), List.of(),
			"sacol", "Colombia", "co", "trade_i_baci_a_22", "2023"
		);

		assertEquals(100, resultado.socios().stream().mapToInt(Socio::porcentaje).sum());
		assertEquals(34, buscarSocio(resultado.socios(), "eeuu").porcentaje());
		assertEquals(33, buscarSocio(resultado.socios(), "china").porcentaje());
		assertEquals(33, buscarSocio(resultado.socios(), "ue").porcentaje());
		assertEquals(0, buscarSocio(resultado.socios(), "otros").porcentaje());
	}

	@Test
	void categoriasQuedanEstimadasCuandoNoHaySecciones() {
		List<JsonNode> filasExp = List.of(fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":100}"));
		List<JsonNode> filasImp = List.of(fila("{\"Exporter Country ID\":\"nausa\",\"Trade Value\":100}"));

		Dependencia resultado = transformer.transformar(
			filasExp, filasImp, List.of(), List.of(),
			"sacol", "Colombia", "co", "trade_i_baci_a_22", "2023"
		);

		assertFalse(resultado.meta().haySecciones());
		assertFalse(buscarCategoria(resultado.categorias(), "Comercio").estimado(), "Comercio usa cuotaPotenciasTotal, no depende de secciones");
		assertTrue(buscarCategoria(resultado.categorias(), "Tecnología").estimado());
		assertTrue(buscarCategoria(resultado.categorias(), "Inversión").estimado(), "Inversión no tiene fuente directa en la OEC");
	}

	@Test
	void categoriasUsanDatosRealesCuandoHaySeccionesDisponibles() {
		List<JsonNode> filasExp = List.of(fila("{\"Importer Country ID\":\"nausa\",\"Trade Value\":100}"));
		List<JsonNode> filasImp = List.of(fila("{\"Exporter Country ID\":\"nausa\",\"Trade Value\":100}"));
		// Sección 16 (Tecnología): 50 con eeuu (export) + 50 con china (import) = 100% potencias.
		List<JsonNode> filasExpSeccion = List.of(fila("{\"Section ID\":\"16\",\"Importer Country ID\":\"nausa\",\"Trade Value\":50}"));
		List<JsonNode> filasImpSeccion = List.of(fila("{\"Section ID\":\"16\",\"Exporter Country ID\":\"aschn\",\"Trade Value\":50}"));

		Dependencia resultado = transformer.transformar(
			filasExp, filasImp, filasExpSeccion, filasImpSeccion,
			"sacol", "Colombia", "co", "trade_i_baci_a_22", "2023"
		);

		assertTrue(resultado.meta().haySecciones());
		Categoria tecnologia = buscarCategoria(resultado.categorias(), "Tecnología");
		assertFalse(tecnologia.estimado());
		assertEquals(100, tecnologia.valor());
	}

	private Socio buscarSocio(List<Socio> socios, String clave) {
		return socios.stream().filter(s -> s.clave().equals(clave)).findFirst().orElseThrow();
	}

	private Categoria buscarCategoria(List<Categoria> categorias, String nombre) {
		return categorias.stream().filter(c -> c.nombre().equals(nombre)).findFirst().orElseThrow();
	}
}
