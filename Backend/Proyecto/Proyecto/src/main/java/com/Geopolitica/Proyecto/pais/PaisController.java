package com.Geopolitica.Proyecto.pais;

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
