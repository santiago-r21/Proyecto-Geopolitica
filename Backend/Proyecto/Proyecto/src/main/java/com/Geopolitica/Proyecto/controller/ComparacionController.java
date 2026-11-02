package com.Geopolitica.Proyecto.controller;

import com.Geopolitica.Proyecto.dto.Dependencia;
import com.Geopolitica.Proyecto.service.DependenciaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ComparacionController {

	private final DependenciaService dependenciaService;

	public ComparacionController(DependenciaService dependenciaService) {
		this.dependenciaService = dependenciaService;
	}

	/** @param codigo id OEC del país (ej. "sacol"), el mismo que devuelve GET /api/paises */
	@GetMapping("/api/paises/{codigo}/comparacion")
	public List<Dependencia> obtener(@PathVariable String codigo) {
		return dependenciaService.obtenerComparacion(codigo);
	}
}
