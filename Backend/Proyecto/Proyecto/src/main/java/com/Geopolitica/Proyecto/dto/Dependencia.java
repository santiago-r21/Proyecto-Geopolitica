package com.Geopolitica.Proyecto.dto;

import java.util.List;

/** Resumen de dependencia económica/estratégica de un país frente a EE.UU./China/UE. */
public record Dependencia(
	String codigo,
	String nombre,
	String iso,
	String subtitulo,
	int indiceDependencia,
	Vulnerabilidad vulnerabilidad,
	Pesos pesos,
	List<Socio> socios,
	List<Categoria> categorias,
	List<SocioDetalle> detalleSocios,
	Meta meta
) {
}
