package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metcoffee.model.Producto;
import com.metcoffee.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

	@Mock
	ProductoRepository productos;

	@InjectMocks
	CatalogoService service;

	@Test
	void consultaProductosActivos() {
		Producto producto = new Producto("1", "Café", 10, 100);
		when(productos.findByActivoTrue()).thenReturn(List.of(producto));

		assertEquals(1, service.consultar().size());

		verify(productos).findByActivoTrue();
	}

	@Test
	void consultaNovedades() {
		Producto producto = new Producto("1", "Café", 10, 100);
		producto.setNovedad(true);
		when(productos.findByActivoTrueAndNovedadTrue()).thenReturn(List.of(producto));

		assertEquals(1, service.novedades().size());

		verify(productos).findByActivoTrueAndNovedadTrue();
	}

	@Test
	void rechazaDetalleDeProductoInactivo() {
		Producto producto = new Producto("1", "Café", 10, 100);
		producto.setActivo(false);
		when(productos.findById("1")).thenReturn(Optional.of(producto));

		assertThrows(IllegalArgumentException.class, () -> service.detalle("1"));
	}
}
