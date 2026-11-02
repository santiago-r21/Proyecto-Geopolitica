package com.Geopolitica.Proyecto.dto;

/** Un bloque de "Dependencia por categoría" (Comercio, Tecnología, Energía...). */
public record Categoria(
	String nombre,
	String icono,
	String acento,
	int valor,
	String nivel,
	String etiqueta,
	String severity,
	String borde,
	boolean estimado
) {
}
