package com.Geopolitica.Proyecto.dto;

import java.util.List;

/** Un socio (bloque) con el desglose de sus áreas de influencia, para "Detalle por Socio Principal". */
public record SocioDetalle(
	String clave,
	String nombre,
	String iso,
	String color,
	String oec,
	int porcentaje,
	List<Area> areas
) {
}
