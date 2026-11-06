package com.metcoffee.controller;

import java.util.List;
import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.metcoffee.dto.ProductoDTO;
import com.metcoffee.service.ProductoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/productos")
public class ProductoController {

	private final ProductoService productoService;

	public ProductoController(ProductoService productoService) {
		this.productoService = productoService;
	}

	// GET /api/admin/productos - Todos los productos (activos e inactivos)
	@GetMapping
	public ResponseEntity<List<ProductoDTO>> listar() {
		return ResponseEntity.ok(productoService.listar());
	}

	// POST /api/admin/productos - Crear producto
	@PostMapping
	public ResponseEntity<ProductoDTO> crear(@Valid @RequestBody ProductoDTO dto) {
		return new ResponseEntity<>(productoService.guardar(dto), HttpStatus.CREATED);
	}

	// PUT /api/admin/productos/{id} - Editar producto
	@PutMapping("/{id}")
	public ResponseEntity<ProductoDTO> actualizar(@PathVariable String id, @Valid @RequestBody ProductoDTO dto) {
		ProductoDTO conId = new ProductoDTO(id, dto.nombre(), dto.origen(), dto.perfil(), dto.imagen(),
				dto.precioBase(), dto.stockGramos(), dto.novedad(), dto.activo());
		return ResponseEntity.ok(productoService.guardar(conId));
	}

	// PUT /api/admin/productos/{id}/imagen - Subir imagen del producto
	@PutMapping("/{id}/imagen")
	public ResponseEntity<ProductoDTO> subirImagen(@PathVariable String id, @RequestParam("archivo") MultipartFile archivo) {
		try {
			return ResponseEntity.ok(productoService.subirImagen(id, archivo.getOriginalFilename(), archivo.getSize(),
					archivo.getBytes()));
		} catch (IOException ex) {
			throw new IllegalStateException("No se pudo leer la imagen", ex);
		}
	}

	// DELETE /api/admin/productos/{id} - Desactivar producto (no se borra, deja de mostrarse en el catálogo)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> desactivar(@PathVariable String id) {
		productoService.desactivar(id);
		return ResponseEntity.noContent().build();
	}
}
