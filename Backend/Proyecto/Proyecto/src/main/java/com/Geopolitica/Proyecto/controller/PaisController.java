package com.Geopolitica.Proyecto.controller;

import com.Geopolitica.Proyecto.dto.Pais;
import com.Geopolitica.Proyecto.service.PaisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PaisController {

	private final PaisService paisService;

	public PaisController(PaisService paisService) {
		this.paisService = paisService;
	}

	@GetMapping("/api/paises")
	public List<Pais> listar() {
		return paisService.obtenerCatalogo();
	}
}
