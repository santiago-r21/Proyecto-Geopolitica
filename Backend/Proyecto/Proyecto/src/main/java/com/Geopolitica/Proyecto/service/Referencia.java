package com.Geopolitica.Proyecto.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Datos estáticos para calcular la dependencia de un país (socios, umbrales,
 * categorías, áreas). Cambiar umbrales/categorías/áreas se resuelve tocando
 * SÓLO este archivo y {@link DependenciaTransformer}.
 */
public final class Referencia {

	private Referencia() {
	}

	public static final String SUBTITULO_PAIS = "Dependencia económica y estratégica";

	/** Uno de los 4 bloques del dashboard. {@code oec} es null para "ue"/"otros" (no son un único id OEC). */
	public record InfoSocio(String clave, String nombre, String iso, String color, String oec) {
	}

	public static final Map<String, InfoSocio> SOCIOS = Map.of(
		"eeuu", new InfoSocio("eeuu", "Estados Unidos", "us", "#0284c7", "nausa"),
		"china", new InfoSocio("china", "China", "cn", "#e11d48", "aschn"),
		"ue", new InfoSocio("ue", "Unión Europea", "eu", "#10b981", null),
		"otros", new InfoSocio("otros", "Otros Países", null, "#8b5cf6", null)
	);

	public static final List<String> ORDEN_SOCIOS = List.of("eeuu", "china", "ue", "otros");

	/** Los 3 bloques que se consideran "potencias" (Otros Países es residual). */
	public static final List<String> POTENCIAS = List.of("eeuu", "china", "ue");

	/**
	 * Claves OEC de los 27 estados miembro de la UE (prefijo {@code eu} + ISO3).
	 * Ojo: en la OEC el prefijo "eu" significa "Europa" (incluye Reino Unido, Rusia,
	 * Suiza, Noruega...), por eso hace falta esta lista explícita para aislar la UE-27.
	 */
	public static final Set<String> CLAVES_UE27 = Set.of(
		"euaut", "eubel", "eubgr", "euhrv", "eucyp", "eucze", "eudnk", "euest", "eufin",
		"eufra", "eudeu", "eugrc", "euhun", "euirl", "euita", "eulva", "eultu", "eulux",
		"eumlt", "eunld", "eupol", "euprt", "eurou", "eusvk", "eusvn", "euesp", "euswe"
	);

	/** Niveles de dependencia -> severidad de PrimeVue Tag + color de borde Tailwind. */
	public record NivelInfo(String etiqueta, String severity, String borde) {
	}

	public static final Map<String, NivelInfo> NIVELES = Map.of(
		"Alta", new NivelInfo("Alta", "danger", "border-rose-200"),
		"Media", new NivelInfo("Media", "warn", "border-amber-200"),
		"Baja", new NivelInfo("Baja", "success", "border-emerald-200")
	);

	/** Umbrales (0-1) para un dominio completo: parte que depende de las 3 potencias. */
	public static String nivelPorCuota(double cuota) {
		if (cuota >= 0.6) return "Alta";
		if (cuota >= 0.35) return "Media";
		return "Baja";
	}

	/** Umbrales (0-1) para la cuota de UN socio individual (más exigentes). */
	public static String nivelPorCuotaSocio(double cuota) {
		if (cuota >= 0.45) return "Alta";
		if (cuota >= 0.22) return "Media";
		return "Baja";
	}

	/** Cómo se calcula la cuota de una categoría/área. */
	public enum Alcance {
		/** Todo el comercio (sin desglose por sección). */
		TODO,
		/** Cuota real calculada a partir de secciones HS concretas (con fallback a ESTIMADO si no hay datos). */
		SECCIONES,
		/** Sin fuente directa en la OEC: se deriva del índice/cuota global + un offset. */
		ESTIMADO,
	}

	/**
	 * Categoría del bloque "Dependencia por categoría".
	 * {@code secciones}: ids de Sección HS (nivel Section de la OEC, 1..21) con los que se
	 * calcula la concentración real; vacío/null cuando no aplica.
	 */
	public record CategoriaDef(String nombre, String icono, String acento, Alcance alcance, List<String> secciones, Double offset) {
	}

	public static final List<CategoriaDef> CATEGORIAS = List.of(
		new CategoriaDef("Comercio", "pi pi-shopping-cart", "sky", Alcance.TODO, null, null),
		new CategoriaDef("Inversión", "pi pi-dollar", "emerald", Alcance.ESTIMADO, null, -19.0),
		new CategoriaDef("Tecnología", "pi pi-microchip", "indigo", Alcance.SECCIONES, List.of("16", "18"), null),
		new CategoriaDef("Energía", "pi pi-bolt", "amber", Alcance.SECCIONES, List.of("5"), null),
		new CategoriaDef("Infraestructura", "pi pi-building", "purple", Alcance.ESTIMADO, null, -27.0),
		new CategoriaDef("Recursos estratégicos", "pi pi-sparkles", "teal", Alcance.SECCIONES, List.of("14", "15"), null)
	);

	/** Cómo se calcula el nivel de un área de influencia de un socio. */
	public enum Metrica {
		EXPORTACION, IMPORTACION, SECCIONES, ESTIMADO,
	}

	/**
	 * Área de influencia mostrada por cada socio en "Detalle por Socio Principal".
	 *   - EXPORTACION/IMPORTACION: cuota real del socio en las exportaciones/importaciones totales.
	 *   - SECCIONES: cuota real del socio dentro de esas secciones HS.
	 *   - ESTIMADO: derivado de la cuota global del socio + offset.
	 */
	public record AreaDef(String nombre, Metrica metrica, List<String> secciones, Double offset) {
	}

	public static final List<AreaDef> AREAS_SOCIO = List.of(
		new AreaDef("Exportaciones", Metrica.EXPORTACION, null, null),
		new AreaDef("Importaciones", Metrica.IMPORTACION, null, null),
		new AreaDef("Tecnología", Metrica.SECCIONES, List.of("16", "18"), null),
		new AreaDef("Inversiones", Metrica.ESTIMADO, null, -0.05),
		new AreaDef("Energía", Metrica.SECCIONES, List.of("5"), null),
		new AreaDef("Infraestructura", Metrica.ESTIMADO, null, -0.12),
		new AreaDef("Recursos estratégicos", Metrica.SECCIONES, List.of("14", "15"), null)
	);
}
