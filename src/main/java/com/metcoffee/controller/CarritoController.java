package com.metcoffee.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.metcoffee.dto.ActualizarItemRequest;
import com.metcoffee.dto.BolsaDTO;
import com.metcoffee.dto.CarritoDTO;
import com.metcoffee.dto.ItemProductoRequest;
import com.metcoffee.service.BolsaPersonalizadaService;
import com.metcoffee.service.CarritoService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import com.metcoffee.service.UsuarioAutorizacionService;

@RestController
@RequestMapping("/api/carrito/{usuarioId}")
public class CarritoController {

	private final CarritoService carritoService;
	private final BolsaPersonalizadaService bolsaService;
	private final UsuarioAutorizacionService autorizacion;

	public CarritoController(CarritoService carritoService, BolsaPersonalizadaService bolsaService,
			UsuarioAutorizacionService autorizacion) {
		this.carritoService = carritoService;
		this.bolsaService = bolsaService;
		this.autorizacion = autorizacion;
	}

	// GET /api/carrito/{usuarioId} - Consultar el carrito
	@GetMapping
	public ResponseEntity<CarritoDTO> consultar(@PathVariable String usuarioId, Authentication authentication) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		return ResponseEntity.ok(carritoService.consultar(usuarioId));
	}

	// POST /api/carrito/{usuarioId}/productos - Agregar un producto (125, 250 o 500 g)
	@PostMapping("/productos")
	public ResponseEntity<CarritoDTO> agregarProducto(@PathVariable String usuarioId,
			Authentication authentication, @Valid @RequestBody ItemProductoRequest request) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		CarritoDTO carrito = carritoService.agregarProducto(usuarioId, request.productoId(), request.cantidad(),
				request.molienda(), request.gramos());
		return new ResponseEntity<>(carrito, HttpStatus.CREATED);
	}

	// POST /api/carrito/{usuarioId}/bolsas - Configurar y agregar una bolsa personalizada
	@PostMapping("/bolsas")
	public ResponseEntity<CarritoDTO> agregarBolsa(@PathVariable String usuarioId, Authentication authentication,
			@Valid @RequestBody BolsaDTO dto) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		CarritoDTO carrito = carritoService.agregarBolsa(usuarioId, bolsaService.configurar(dto));
		return new ResponseEntity<>(carrito, HttpStatus.CREATED);
	}

	// PUT /api/carrito/{usuarioId}/items/{indice} - Actualizar un producto del carrito
	@PutMapping("/items/{indice}")
	public ResponseEntity<CarritoDTO> actualizar(@PathVariable String usuarioId, @PathVariable int indice,
			Authentication authentication, @Valid @RequestBody ActualizarItemRequest request) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		return ResponseEntity.ok(carritoService.actualizarProducto(usuarioId, indice, request.cantidad(),
				request.molienda(), request.gramos()));
	}

	// DELETE /api/carrito/{usuarioId}/items/{indice} - Quitar un ítem
	@DeleteMapping("/items/{indice}")
	public ResponseEntity<Void> eliminar(@PathVariable String usuarioId, @PathVariable int indice,
			Authentication authentication) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		carritoService.eliminar(usuarioId, indice);
		return ResponseEntity.noContent().build();
	}

	// DELETE /api/carrito/{usuarioId} - Vaciar el carrito
	@DeleteMapping
	public ResponseEntity<Void> limpiar(@PathVariable String usuarioId, Authentication authentication) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		carritoService.limpiar(usuarioId);
		return ResponseEntity.noContent().build();
	}
}
