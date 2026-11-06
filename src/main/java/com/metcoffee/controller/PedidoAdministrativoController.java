package com.metcoffee.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.metcoffee.dto.EstadoPedidoRequest;
import com.metcoffee.dto.PedidoDTO;
import com.metcoffee.service.PedidoAdministrativoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/pedidos")
public class PedidoAdministrativoController {

	private final PedidoAdministrativoService pedidoService;

	public PedidoAdministrativoController(PedidoAdministrativoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	// GET /api/admin/pedidos - Todos los pedidos
	@GetMapping
	public ResponseEntity<List<PedidoDTO>> listar() {
		return ResponseEntity.ok(pedidoService.listar());
	}

	// GET /api/admin/pedidos/{id} - Detalle de un pedido
	@GetMapping("/{id}")
	public ResponseEntity<PedidoDTO> detalle(@PathVariable String id) {
		return ResponseEntity.ok(pedidoService.detalle(id));
	}

	// PUT /api/admin/pedidos/{id}/estado - Cambiar estado (Procesando→Enviado|Cancelado, Enviado→Entregado)
	@PutMapping("/{id}/estado")
	public ResponseEntity<PedidoDTO> actualizarEstado(@PathVariable String id,
			@Valid @RequestBody EstadoPedidoRequest request) {
		return ResponseEntity.ok(pedidoService.actualizarEstado(id, request.estado()));
	}
}
