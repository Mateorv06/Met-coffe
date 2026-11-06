package com.metcoffee.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.metcoffee.dto.DireccionDTO;
import com.metcoffee.service.DomicilioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import com.metcoffee.service.UsuarioAutorizacionService;

@RestController
@RequestMapping("/api/domicilios/{usuarioId}")
public class DomicilioController {

	private final DomicilioService domicilioService;
	private final UsuarioAutorizacionService autorizacion;

	public DomicilioController(DomicilioService domicilioService, UsuarioAutorizacionService autorizacion) {
		this.domicilioService = domicilioService;
		this.autorizacion = autorizacion;
	}

	// GET /api/domicilios/{usuarioId} - Listar domicilios del usuario
	@GetMapping
	public ResponseEntity<List<DireccionDTO>> listar(@PathVariable String usuarioId, Authentication authentication) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		return ResponseEntity.ok(domicilioService.listar(usuarioId));
	}

	// POST /api/domicilios/{usuarioId} - Crear domicilio
	@PostMapping
	public ResponseEntity<DireccionDTO> guardar(@PathVariable String usuarioId, Authentication authentication,
			@Valid @RequestBody DireccionDTO dto) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		return new ResponseEntity<>(domicilioService.guardar(usuarioId, dto), HttpStatus.CREATED);
	}

	// PUT /api/domicilios/{usuarioId}/{id} - Actualizar domicilio
	@PutMapping("/{id}")
	public ResponseEntity<DireccionDTO> actualizar(@PathVariable String usuarioId, @PathVariable String id,
			Authentication authentication, @Valid @RequestBody DireccionDTO dto) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		return ResponseEntity.ok(domicilioService.actualizar(usuarioId, id, dto));
	}

	// PUT /api/domicilios/{usuarioId}/{id}/principal - Marcar como domicilio principal
	@PutMapping("/{id}/principal")
	public ResponseEntity<Void> seleccionarPrincipal(@PathVariable String usuarioId, @PathVariable String id,
			Authentication authentication) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		domicilioService.seleccionarPrincipal(usuarioId, id);
		return ResponseEntity.noContent().build();
	}
}
