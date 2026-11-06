package com.metcoffee.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.metcoffee.dto.CambioRolRequest;
import com.metcoffee.dto.UsuarioDTO;
import com.metcoffee.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	// GET /api/admin/usuarios - Listar usuarios
	@GetMapping
	public ResponseEntity<List<UsuarioDTO>> listar() {
		return ResponseEntity.ok(usuarioService.listar());
	}

	// GET /api/admin/usuarios/{id} - Consultar un usuario
	@GetMapping("/{id}")
	public ResponseEntity<UsuarioDTO> consultar(@PathVariable String id) {
		return ResponseEntity.ok(usuarioService.consultar(id));
	}

	// PUT /api/admin/usuarios/{id}/rol - Cambiar rol (no el propio)
	@PutMapping("/{id}/rol")
	public ResponseEntity<UsuarioDTO> cambiarRol(@PathVariable String id, Authentication authentication,
			@Valid @RequestBody CambioRolRequest request) {
		return ResponseEntity.ok(usuarioService.cambiarRol(authentication.getName(), id, request.rol()));
	}

	// DELETE /api/admin/usuarios/{id} - Eliminar (no el propio ni con pedidos)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable String id, Authentication authentication) {
		usuarioService.eliminar(authentication.getName(), id);
		return ResponseEntity.noContent().build();
	}
}
