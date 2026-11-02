package com.Geopolitica.Proyecto.dto;

/** Comercio de un país con un socio real (sin agrupar en bloques), para el mapa interactivo. */
public record SocioPais(
	String codigo,
	String nombre,
	String iso2,
	double exportaciones,
	double importaciones,
	double comercioTotal,
	int cuotaPorcentaje
) {
}
