package com.Geopolitica.Proyecto.controller;

import com.Geopolitica.Proyecto.client.OecProperties;
import com.Geopolitica.Proyecto.dto.Dependencia;
import com.Geopolitica.Proyecto.service.DependenciaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DependenciaController {

	private final DependenciaService dependenciaService;
	private final OecProperties propiedades;

	public DependenciaController(DependenciaService dependenciaService, OecProperties propiedades) {
		this.dependenciaService = dependenciaService;
		this.propiedades = propiedades;
	}

	/** @param codigo id OEC del país (ej. "sacol"), el mismo que devuelve GET /api/paises */
	@GetMapping("/api/paises/{codigo}/dependencia")
	public Dependencia obtener(@PathVariable String codigo, @RequestParam(required = false) String anio) {
		return dependenciaService.obtener(codigo, anio != null ? anio : propiedades.anio());
	}
}
