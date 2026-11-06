package com.metcoffee.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.metcoffee.dto.ProductoDTO;
import com.metcoffee.service.CatalogoService;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

	private final CatalogoService catalogoService;

	public CatalogoController(CatalogoService catalogoService) {
		this.catalogoService = catalogoService;
	}

	// GET /api/catalogo - Productos activos
	@GetMapping
	public ResponseEntity<List<ProductoDTO>> consultar() {
		return ResponseEntity.ok(catalogoService.consultar());
	}

	// GET /api/catalogo/novedades - Productos marcados como novedad
	@GetMapping("/novedades")
	public ResponseEntity<List<ProductoDTO>> novedades() {
		return ResponseEntity.ok(catalogoService.novedades());
	}

	// GET /api/catalogo/{id} - Detalle de un producto
	@GetMapping("/{id}")
	public ResponseEntity<ProductoDTO> detalle(@PathVariable String id) {
		return ResponseEntity.ok(catalogoService.detalle(id));
	}
}
