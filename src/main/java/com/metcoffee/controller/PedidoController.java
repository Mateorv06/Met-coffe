package com.metcoffee.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.metcoffee.dto.PedidoDTO;
import com.metcoffee.service.PedidoService;
import com.metcoffee.service.UsuarioAutorizacionService;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/pedidos/{usuarioId}")
public class PedidoController {

	private final PedidoService pedidoService;
	private final UsuarioAutorizacionService autorizacion;

	public PedidoController(PedidoService pedidoService, UsuarioAutorizacionService autorizacion) {
		this.pedidoService = pedidoService;
		this.autorizacion = autorizacion;
	}

	// GET /api/pedidos/{usuarioId} - Historial de pedidos del usuario
	@GetMapping
	public ResponseEntity<List<PedidoDTO>> historial(@PathVariable String usuarioId, Authentication authentication) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		return ResponseEntity.ok(pedidoService.historial(usuarioId));
	}

	// GET /api/pedidos/{usuarioId}/{id} - Detalle de un pedido
	@GetMapping("/{id}")
	public ResponseEntity<PedidoDTO> detalle(@PathVariable String usuarioId, @PathVariable String id,
			Authentication authentication) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		return ResponseEntity.ok(pedidoService.detalle(usuarioId, id));
	}

	// DELETE /api/pedidos/{usuarioId}/{id} - Cancelar (solo si está en Procesando)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancelar(@PathVariable String usuarioId, @PathVariable String id,
			Authentication authentication) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		pedidoService.cancelar(usuarioId, id);
		return ResponseEntity.noContent().build();
	}
}
