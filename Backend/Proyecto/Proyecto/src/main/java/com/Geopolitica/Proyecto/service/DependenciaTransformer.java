package com.Geopolitica.Proyecto.service;

import com.Geopolitica.Proyecto.dto.Area;
import com.Geopolitica.Proyecto.dto.Categoria;
import com.Geopolitica.Proyecto.dto.Dependencia;
import com.Geopolitica.Proyecto.dto.Meta;
import com.Geopolitica.Proyecto.dto.Pesos;
import com.Geopolitica.Proyecto.dto.Socio;
import com.Geopolitica.Proyecto.dto.SocioDetalle;
import com.Geopolitica.Proyecto.dto.Vulnerabilidad;
import com.Geopolitica.Proyecto.service.Referencia.AreaDef;
import com.Geopolitica.Proyecto.service.Referencia.CategoriaDef;
import com.Geopolitica.Proyecto.service.Referencia.InfoSocio;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte las filas crudas de la OEC en el modelo que consumen los componentes del
 * dashboard. Puerto de Frontend/src/services/oec/transform.js: TODAS las fórmulas y
 * umbrales del dashboard viven aquí y en {@link Referencia}, y son ajustables ahí.
 *
 * Concepto: "dependencia" = exposición del país a las grandes potencias
 * (EE.UU., China, Unión Europea). "Otros Países" es el residual (resto del mundo).
 */
@Component
public class DependenciaTransformer {

	// Pesos del índice de dependencia (deben sumar 1).
	private static final double PESO_POTENCIA_LIDER = 0.55;
	private static final double PESO_POTENCIAS = 0.30;
	private static final double PESO_CONCENTRACION = 0.15;
	// Curva de sensibilidad (<1 realza los valores medios-altos sin alterar el orden).
	private static final double CURVA_INDICE = 0.8;
	// Umbrales del índice (0-100) para la etiqueta de vulnerabilidad.
	private static final int UMBRAL_VULNERABILIDAD_ALTA = 65;
	private static final int UMBRAL_VULNERABILIDAD_MEDIA = 45;

	private static final double OFFSET_CATEGORIA_DEFECTO = -15.0;
	private static final double OFFSET_AREA_DEFECTO = -0.05;

	/**
	 * @param filasExp         exportaciones del país, por socio (OBLIGATORIA)
	 * @param filasImp         importaciones del país, por socio (OBLIGATORIA)
	 * @param filasExpSeccion  exportaciones por Sección HS × socio (best-effort, puede venir vacía)
	 * @param filasImpSeccion  importaciones por Sección HS × socio (best-effort, puede venir vacía)
	 * @param codigo           id OEC del país consultado (ej. "sacol")
	 * @param nombre           nombre del país (del catálogo)
	 * @param iso              ISO2 del país (del catálogo, para la bandera)
	 */
	public Dependencia transformar(
		List<JsonNode> filasExp,
		List<JsonNode> filasImp,
		List<JsonNode> filasExpSeccion,
		List<JsonNode> filasImpSeccion,
		String codigo,
		String nombre,
		String iso,
		String cube,
		String anio
	) {
		boolean haySecciones = !filasExpSeccion.isEmpty() || !filasImpSeccion.isEmpty();

		Map<String, Double> expPorBloque = sumarPorBloque(filasExp, "Importer Country ID");
		Map<String, Double> impPorBloque = sumarPorBloque(filasImp, "Exporter Country ID");
		double exportacionTotal = sumar(expPorBloque);
		double importacionTotal = sumar(impPorBloque);

		Map<String, Double> combinado = bloquesVacios();
		for (String k : Referencia.ORDEN_SOCIOS) {
			combinado.put(k, expPorBloque.get(k) + impPorBloque.get(k));
		}
		double comercioTotal = sumar(combinado);

		Map<String, Double> cuotaPorBloque = bloquesVacios();
		Map<String, Double> cuotaExportacion = bloquesVacios();
		Map<String, Double> cuotaImportacion = bloquesVacios();
		for (String k : Referencia.ORDEN_SOCIOS) {
			cuotaPorBloque.put(k, comercioTotal > 0 ? combinado.get(k) / comercioTotal : 0.0);
			cuotaExportacion.put(k, exportacionTotal > 0 ? expPorBloque.get(k) / exportacionTotal : 0.0);
			cuotaImportacion.put(k, importacionTotal > 0 ? impPorBloque.get(k) / importacionTotal : 0.0);
		}

		// --- Índice de dependencia (derivado) ---
		double potenciaLider = Referencia.POTENCIAS.stream().mapToDouble(cuotaPorBloque::get).max().orElse(0);
		double sumaPotencias = Referencia.POTENCIAS.stream().mapToDouble(cuotaPorBloque::get).sum();
		// Concentración ENTRE las potencias (HHI renormalizado, 0.33 = reparto equitativo, 1 = una sola).
		double hhiPotencias = sumaPotencias > 0
			? Referencia.POTENCIAS.stream().mapToDouble(k -> Math.pow(cuotaPorBloque.get(k) / sumaPotencias, 2)).sum()
			: 0;
		double indiceBruto = PESO_POTENCIA_LIDER * potenciaLider + PESO_POTENCIAS * sumaPotencias + PESO_CONCENTRACION * hhiPotencias;
		int indiceDependencia = (int) acotar(Math.round(100 * Math.pow(indiceBruto, CURVA_INDICE)), 0, 100);

		Map<String, Integer> porcentajes = aPorcentajes(cuotaPorBloque);
		List<String> orden = ordenarBloques(cuotaPorBloque);

		List<Socio> socios = orden.stream().map(k -> aSocio(k, porcentajes)).toList();

		Map<String, Map<String, Double>> bloquesPorSeccion = agruparPorSeccion(filasExpSeccion, filasImpSeccion);
		double cuotaPotenciasTotal = comercioTotal > 0 ? sumaPotencias : 0;

		List<Categoria> categorias = construirCategorias(indiceDependencia, cuotaPotenciasTotal, bloquesPorSeccion, haySecciones);

		List<SocioDetalle> detalleSocios = orden.stream()
			.map(k -> aSocioDetalle(k, porcentajes, cuotaExportacion, cuotaImportacion, bloquesPorSeccion, haySecciones, cuotaPorBloque))
			.toList();

		Meta meta = new Meta("OEC", "BACI · HS-2022", cube, anio, haySecciones, Instant.now().toString());

		return new Dependencia(
			codigo, nombre, iso, Referencia.SUBTITULO_PAIS,
			indiceDependencia, vulnerabilidadPara(indiceDependencia), new Pesos(50, 50),
			socios, categorias, detalleSocios, meta
		);
	}

	private Socio aSocio(String clave, Map<String, Integer> porcentajes) {
		InfoSocio info = Referencia.SOCIOS.get(clave);
		return new Socio(info.clave(), info.nombre(), info.iso(), info.color(), info.oec(), porcentajes.get(clave));
	}

	private SocioDetalle aSocioDetalle(
		String clave,
		Map<String, Integer> porcentajes,
		Map<String, Double> cuotaExportacion,
		Map<String, Double> cuotaImportacion,
		Map<String, Map<String, Double>> bloquesPorSeccion,
		boolean haySecciones,
		Map<String, Double> cuotaPorBloque
	) {
		InfoSocio info = Referencia.SOCIOS.get(clave);
		List<Area> areas = construirAreasSocio(clave, cuotaExportacion, cuotaImportacion, bloquesPorSeccion, haySecciones, cuotaPorBloque);
		return new SocioDetalle(info.clave(), info.nombre(), info.iso(), info.color(), info.oec(), porcentajes.get(clave), areas);
	}

	/** Clasifica un id de país OEC en uno de los 4 bloques del dashboard. */
	private String bloqueDe(String oecId) {
		if (oecId.equals(Referencia.SOCIOS.get("eeuu").oec())) return "eeuu";
		if (oecId.equals(Referencia.SOCIOS.get("china").oec())) return "china";
		if (Referencia.CLAVES_UE27.contains(oecId)) return "ue";
		return "otros";
	}

	/** Orden de presentación: potencias por cuota descendente y "Otros" siempre al final. */
	private List<String> ordenarBloques(Map<String, Double> cuotaPorBloque) {
		List<String> potencias = new ArrayList<>(Referencia.POTENCIAS);
		potencias.sort(Comparator.comparingDouble((String k) -> cuotaPorBloque.get(k)).reversed());
		List<String> orden = new ArrayList<>(potencias);
		orden.add("otros");
		return orden;
	}

	/** Reparte 100 puntos entre bloques con el método del mayor resto (evita 99/101%). */
	private Map<String, Integer> aPorcentajes(Map<String, Double> cuotaPorBloque) {
		List<String> claves = Referencia.ORDEN_SOCIOS;
		int n = claves.size();
		int[] enteros = new int[n];
		double[] restos = new double[n];
		for (int i = 0; i < n; i++) {
			double exacto = cuotaPorBloque.getOrDefault(claves.get(i), 0.0) * 100;
			enteros[i] = (int) Math.floor(exacto);
			restos[i] = exacto - enteros[i];
		}
		int restantes = 100 - Arrays.stream(enteros).sum();

		Integer[] indicesOrdenados = new Integer[n];
		for (int i = 0; i < n; i++) indicesOrdenados[i] = i;
		Arrays.sort(indicesOrdenados, (a, b) -> Double.compare(restos[b], restos[a]));

		for (int i = 0; i < n && restantes > 0; i++, restantes--) {
			enteros[indicesOrdenados[i]] += 1;
		}

		Map<String, Integer> resultado = new LinkedHashMap<>();
		for (int i = 0; i < n; i++) {
			resultado.put(claves.get(i), Math.max(0, enteros[i]));
		}
		return resultado;
	}

	/** Suma `Trade Value` por bloque a partir de filas con un campo id concreto. */
	private Map<String, Double> sumarPorBloque(List<JsonNode> filas, String campoId) {
		Map<String, Double> acc = bloquesVacios();
		for (JsonNode fila : filas) {
			String bloque = bloqueDe(fila.path(campoId).asString());
			acc.merge(bloque, aNumero(fila.path("Trade Value")), Double::sum);
		}
		return acc;
	}

	/** { [seccionId]: { eeuu, china, ue, otros } } combinando exportaciones + importaciones. */
	private Map<String, Map<String, Double>> agruparPorSeccion(List<JsonNode> filasExpSeccion, List<JsonNode> filasImpSeccion) {
		Map<String, Map<String, Double>> acc = new HashMap<>();
		acumularPorSeccion(acc, filasExpSeccion, "Importer Country ID");
		acumularPorSeccion(acc, filasImpSeccion, "Exporter Country ID");
		return acc;
	}

	private void acumularPorSeccion(Map<String, Map<String, Double>> acc, List<JsonNode> filas, String campoId) {
		for (JsonNode fila : filas) {
			String seccionId = fila.path("Section ID").asString();
			Map<String, Double> bloques = acc.computeIfAbsent(seccionId, k -> bloquesVacios());
			String bloque = bloqueDe(fila.path(campoId).asString());
			bloques.merge(bloque, aNumero(fila.path("Trade Value")), Double::sum);
		}
	}

	/** Suma los bloques de un conjunto de secciones. */
	private Map<String, Double> sumarSecciones(Map<String, Map<String, Double>> bloquesPorSeccion, List<String> seccionesIds) {
		Map<String, Double> totales = bloquesVacios();
		for (String sid : seccionesIds) {
			Map<String, Double> bloques = bloquesPorSeccion.get(sid);
			if (bloques == null) continue;
			for (String k : Referencia.ORDEN_SOCIOS) {
				totales.merge(k, bloques.get(k), Double::sum);
			}
		}
		return totales;
	}

	/** Parte (0-1) de un dominio comercial que depende de las 3 potencias, o null si no hay comercio. */
	private Double cuotaPotencias(Map<String, Double> totales) {
		double total = sumar(totales);
		if (total <= 0) return null;
		return (totales.get("eeuu") + totales.get("china") + totales.get("ue")) / total;
	}

	private record NivelResultado(String nivel, String etiqueta, String severity, String borde) {
	}

	private NivelResultado conNivel(double cuota, java.util.function.Function<Double, String> calcularNivel) {
		String nivel = calcularNivel.apply(cuota);
		Referencia.NivelInfo info = Referencia.NIVELES.get(nivel);
		return new NivelResultado(nivel, info.etiqueta(), info.severity(), info.borde());
	}

	private List<Categoria> construirCategorias(
		int indice, double cuotaPotenciasTotal, Map<String, Map<String, Double>> bloquesPorSeccion, boolean haySecciones
	) {
		List<Categoria> categorias = new ArrayList<>();
		for (CategoriaDef cat : Referencia.CATEGORIAS) {
			double cuota;
			boolean estimado = false;

			if (cat.alcance() == Referencia.Alcance.TODO) {
				cuota = cuotaPotenciasTotal;
			} else if (cat.alcance() == Referencia.Alcance.SECCIONES && haySecciones) {
				Double real = cuotaPotencias(sumarSecciones(bloquesPorSeccion, cat.secciones()));
				if (real == null) {
					cuota = acotar((indice + offsetCategoria(cat)) / 100.0, 0.05, 0.98);
					estimado = true;
				} else {
					cuota = real;
				}
			} else {
				cuota = acotar((indice + offsetCategoria(cat)) / 100.0, 0.05, 0.98);
				estimado = true;
			}

			NivelResultado nivel = conNivel(cuota, Referencia::nivelPorCuota);
			categorias.add(new Categoria(
				cat.nombre(), cat.icono(), cat.acento(), (int) Math.round(cuota * 100),
				nivel.nivel(), nivel.etiqueta(), nivel.severity(), nivel.borde(), estimado
			));
		}
		return categorias;
	}

	private double offsetCategoria(CategoriaDef cat) {
		return cat.offset() != null ? cat.offset() : OFFSET_CATEGORIA_DEFECTO;
	}

	private record CandidataArea(double cuota, String nivel, Area area) {
	}

	private List<Area> construirAreasSocio(
		String bloque,
		Map<String, Double> cuotaExportacion,
		Map<String, Double> cuotaImportacion,
		Map<String, Map<String, Double>> bloquesPorSeccion,
		boolean haySecciones,
		Map<String, Double> cuotaPorBloque
	) {
		List<CandidataArea> candidatas = new ArrayList<>();
		for (AreaDef area : Referencia.AREAS_SOCIO) {
			double cuota;
			boolean estimado = false;

			if (area.metrica() == Referencia.Metrica.EXPORTACION) {
				cuota = cuotaExportacion.getOrDefault(bloque, 0.0);
			} else if (area.metrica() == Referencia.Metrica.IMPORTACION) {
				cuota = cuotaImportacion.getOrDefault(bloque, 0.0);
			} else if (area.metrica() == Referencia.Metrica.SECCIONES && haySecciones) {
				Map<String, Double> totales = sumarSecciones(bloquesPorSeccion, area.secciones());
				double total = sumar(totales);
				cuota = total > 0 ? totales.get(bloque) / total : 0.0;
			} else {
				double offset = area.offset() != null ? area.offset() : OFFSET_AREA_DEFECTO;
				cuota = acotar(cuotaPorBloque.getOrDefault(bloque, 0.0) + offset, 0.02, 0.95);
				estimado = true;
			}

			NivelResultado nivel = conNivel(cuota, Referencia::nivelPorCuotaSocio);
			Area areaFinal = new Area(area.nombre(), nivel.nivel(), nivel.etiqueta(), nivel.severity(), nivel.borde(), estimado);
			candidatas.add(new CandidataArea(cuota, nivel.nivel(), areaFinal));
		}

		Map<String, Integer> ordenNivel = Map.of("Alta", 0, "Media", 1, "Baja", 2);
		candidatas.sort(
			Comparator.comparingInt((CandidataArea c) -> ordenNivel.get(c.nivel()))
				.thenComparing(Comparator.comparingDouble(CandidataArea::cuota).reversed())
		);

		// 3 áreas mínimo, 5 máximo; descarta las de cuota irrelevante salvo para llegar al mínimo.
		List<CandidataArea> relevantes = candidatas.stream().filter(c -> c.cuota() >= 0.03).toList();
		List<CandidataArea> elegidas = relevantes.size() >= 3 ? relevantes : candidatas;
		return elegidas.stream().limit(5).map(CandidataArea::area).toList();
	}

	private Vulnerabilidad vulnerabilidadPara(int indice) {
		if (indice >= UMBRAL_VULNERABILIDAD_ALTA) return new Vulnerabilidad("Alta Vulnerabilidad", "danger");
		if (indice >= UMBRAL_VULNERABILIDAD_MEDIA) return new Vulnerabilidad("Vulnerabilidad Media", "warn");
		return new Vulnerabilidad("Vulnerabilidad Baja", "success");
	}

	private double aNumero(JsonNode node) {
		return node == null ? 0 : node.asDouble(0);
	}

	private double acotar(double v, double min, double max) {
		return Math.min(max, Math.max(min, v));
	}

	private double sumar(Map<String, Double> valores) {
		return valores.values().stream().mapToDouble(Double::doubleValue).sum();
	}

	private Map<String, Double> bloquesVacios() {
		Map<String, Double> bloques = new LinkedHashMap<>();
		for (String k : Referencia.ORDEN_SOCIOS) bloques.put(k, 0.0);
		return bloques;
	}
}
