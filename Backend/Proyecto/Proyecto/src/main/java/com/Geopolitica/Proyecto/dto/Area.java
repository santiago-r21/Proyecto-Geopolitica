package com.Geopolitica.Proyecto.dto;

/** Área de influencia de un socio (Exportaciones, Tecnología, Energía...) dentro de "Detalle por Socio". */
public record Area(String nombre, String nivel, String etiqueta, String severity, String borde, boolean estimado) {
}
