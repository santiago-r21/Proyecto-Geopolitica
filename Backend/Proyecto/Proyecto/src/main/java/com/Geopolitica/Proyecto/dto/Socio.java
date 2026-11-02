package com.Geopolitica.Proyecto.dto;

/** Uno de los 4 bloques del dashboard (EE.UU./China/UE/Otros) con su cuota (%) para un país. */
public record Socio(String clave, String nombre, String iso, String color, String oec, int porcentaje) {
}
