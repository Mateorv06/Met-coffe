package com.metcoffee.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.metcoffee.dto.ConfirmarCompraRequest;
import com.metcoffee.dto.PedidoDTO;
import com.metcoffee.service.CompraService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import com.metcoffee.service.UsuarioAutorizacionService;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

	private final CompraService compraService;
	private final UsuarioAutorizacionService autorizacion;

	public CompraController(CompraService compraService, UsuarioAutorizacionService autorizacion) {
		this.compraService = compraService;
		this.autorizacion = autorizacion;
	}

	// POST /api/compras/{usuarioId} - Confirmar la compra del carrito (pago simulado + pedido + stock)
	@PostMapping("/{usuarioId}")
	public ResponseEntity<PedidoDTO> confirmar(@PathVariable String usuarioId,
			Authentication authentication, @Valid @RequestBody ConfirmarCompraRequest request) {
		autorizacion.verificarPropietario(usuarioId, authentication);
		return new ResponseEntity<>(compraService.confirmar(usuarioId, request.direccionId()), HttpStatus.CREATED);
	}
}
