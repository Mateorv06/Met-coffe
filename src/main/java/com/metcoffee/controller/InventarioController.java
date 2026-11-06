package com.metcoffee.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.metcoffee.dto.AjusteStockRequest;
import com.metcoffee.model.MovimientoInventario;
import com.metcoffee.model.Producto;
import com.metcoffee.service.InventarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/inventario")
public class InventarioController {

	private final InventarioService inventarioService;

	public InventarioController(InventarioService inventarioService) {
		this.inventarioService = inventarioService;
	}

	// GET /api/admin/inventario/{productoId} - Consultar el stock en gramos de un producto
	@GetMapping("/{productoId}")
	public ResponseEntity<Producto> consultar(@PathVariable String productoId) {
		return ResponseEntity.ok(inventarioService.consultar(productoId));
	}

	// PUT /api/admin/inventario/{productoId} - Ajustar el stock (queda registrado como movimiento)
	@PutMapping("/{productoId}")
	public ResponseEntity<Producto> ajustar(@PathVariable String productoId, @Valid @RequestBody AjusteStockRequest request) {
		return ResponseEntity.ok(inventarioService.ajustar(productoId, request.stockGramos(), request.nota()));
	}

	// GET /api/admin/inventario/movimientos - Historial de movimientos de todo el inventario
	@GetMapping("/movimientos")
	public ResponseEntity<List<MovimientoInventario>> movimientos() {
		return ResponseEntity.ok(inventarioService.listarMovimientos());
	}

	// GET /api/admin/inventario/{productoId}/movimientos - Historial de un producto
	@GetMapping("/{productoId}/movimientos")
	public ResponseEntity<List<MovimientoInventario>> movimientosDeProducto(@PathVariable String productoId) {
		return ResponseEntity.ok(inventarioService.listarMovimientos(productoId));
	}
}
