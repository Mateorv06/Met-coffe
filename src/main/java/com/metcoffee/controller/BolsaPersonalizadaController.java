package com.metcoffee.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.metcoffee.dto.BolsaDTO;
import com.metcoffee.model.BolsaPersonalizada;
import com.metcoffee.service.BolsaPersonalizadaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bolsa")
public class BolsaPersonalizadaController {

	private final BolsaPersonalizadaService bolsaService;

	public BolsaPersonalizadaController(BolsaPersonalizadaService bolsaService) {
		this.bolsaService = bolsaService;
	}

	// POST /api/bolsa/configurar - Valida la configuración y devuelve la bolsa con su precio (CU-03)
	@PostMapping("/configurar")
	public ResponseEntity<BolsaPersonalizada> configurar(@Valid @RequestBody BolsaDTO dto) {
		return ResponseEntity.ok(bolsaService.configurar(dto));
	}
}
