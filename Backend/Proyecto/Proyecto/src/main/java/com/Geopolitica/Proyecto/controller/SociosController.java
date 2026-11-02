package com.Geopolitica.Proyecto.controller;

import com.Geopolitica.Proyecto.client.OecProperties;
import com.Geopolitica.Proyecto.dto.SocioPais;
import com.Geopolitica.Proyecto.service.SociosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SociosController {

	private final SociosService sociosService;
	private final OecProperties propiedades;

	public SociosController(SociosService sociosService, OecProperties propiedades) {
		this.sociosService = sociosService;
		this.propiedades = propiedades;
	}

	/** @param codigo id OEC del país (ej. "sacol"), el mismo que devuelve GET /api/paises */
	@GetMapping("/api/paises/{codigo}/socios")
	public List<SocioPais> obtener(@PathVariable String codigo, @RequestParam(required = false) String anio) {
		return sociosService.obtener(codigo, anio != null ? anio : propiedades.anio());
	}
}
