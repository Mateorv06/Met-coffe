package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metcoffee.model.BolsaPersonalizada;
import com.metcoffee.model.Carrito;
import com.metcoffee.model.CompartimentoBolsa;
import com.metcoffee.model.ItemCarrito;
import com.metcoffee.model.Producto;
import com.metcoffee.repository.CarritoRepository;
import com.metcoffee.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

	@Mock
	CarritoRepository carritos;

	@Mock
	ProductoRepository productos;

	@InjectMocks
	CarritoService service;

	private BolsaPersonalizada bolsa() {
		return new BolsaPersonalizada("mediana", 250,
				List.of(new CompartimentoBolsa("p", "Grano", 125), new CompartimentoBolsa("p", "Molido", 125)), 10);
	}

	@ParameterizedTest
	@CsvSource({ "125,5.0", "250,10.0", "500,18.0" })
	void agregaProductoConPrecioSegunElTamano(double gramos, double precioEsperado) {
		Producto producto = new Producto("p", "Café", 10, 5000);
		when(productos.findById("p")).thenReturn(Optional.of(producto));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.empty());
		when(carritos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var resultado = service.agregarProducto("u", "p", 1, "Grano", gramos);

		assertEquals(precioEsperado, resultado.items().get(0).precioUnitario());
		assertEquals(precioEsperado, resultado.total());
		verify(carritos).save(any());
	}

	@Test
	void rechazaTamanosQueNoSonPresentacionesFijas() {
		assertThrows(IllegalArgumentException.class, () -> service.agregarProducto("u", "p", 1, "Grano", 300));

		verify(carritos, never()).save(any());
	}

	@Test
	void rechazaCantidadFueraDeRango() {
		assertThrows(IllegalArgumentException.class, () -> service.agregarProducto("u", "p", 0, "Grano", 125));
		assertThrows(IllegalArgumentException.class, () -> service.agregarProducto("u", "p", 100, "Grano", 125));
		assertThrows(IllegalArgumentException.class, () -> service.actualizarProducto("u", 0, 100, "Grano", 125));
	}

	@Test
	void rechazaMoliendaInvalida() {
		assertThrows(IllegalArgumentException.class, () -> service.agregarProducto("u", "p", 1, "Fina", 125));
		assertThrows(IllegalArgumentException.class, () -> service.agregarProducto("u", "p", 1, null, 125));
	}

	@Test
	void sumaLaCantidadCuandoLaLineaYaExiste() {
		Producto producto = new Producto("p", "Café", 10, 5000);
		Carrito carrito = new Carrito("c", "u");
		carrito.setItems(List.of(new ItemCarrito("p", 2, "Grano", 250, 10, null)));
		when(productos.findById("p")).thenReturn(Optional.of(producto));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(carritos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var resultado = service.agregarProducto("u", "p", 3, "Grano", 250);

		assertEquals(1, resultado.items().size());
		assertEquals(5, resultado.items().get(0).cantidad());
		assertEquals(50, resultado.total());
	}

	@Test
	void rechazaSuperarLos99UnidadesPorItem() {
		Producto producto = new Producto("p", "Café", 10, 1_000_000);
		Carrito carrito = new Carrito("c", "u");
		carrito.setItems(List.of(new ItemCarrito("p", 98, "Grano", 250, 10, null)));
		when(productos.findById("p")).thenReturn(Optional.of(producto));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));

		assertThrows(IllegalArgumentException.class, () -> service.agregarProducto("u", "p", 2, "Grano", 250));

		verify(carritos, never()).save(any());
	}

	@Test
	void rechazaProductoInactivoOInexistente() {
		Producto inactivo = new Producto("p", "Café", 10, 500);
		inactivo.setActivo(false);
		when(productos.findById("p")).thenReturn(Optional.of(inactivo));
		when(productos.findById("x")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.agregarProducto("u", "p", 1, "Grano", 125));
		assertThrows(IllegalArgumentException.class, () -> service.agregarProducto("u", "x", 1, "Grano", 125));
	}

	@Test
	void rechazaAgregarSiElStockNoAlcanzaContandoLoQueYaHayEnElCarrito() {
		Producto producto = new Producto("p", "Café", 10, 400);
		Carrito carrito = new Carrito("c", "u");
		carrito.setItems(List.of(new ItemCarrito("p", 1, "Molido", 250, 10, null)));
		when(productos.findById("p")).thenReturn(Optional.of(producto));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));

		assertThrows(IllegalStateException.class, () -> service.agregarProducto("u", "p", 1, "Grano", 250));
	}

	@Test
	void actualizaProductoYRecalculaTotal() {
		Producto producto = new Producto("p", "Café", 10, 1000);
		Carrito carrito = new Carrito("c", "u");
		carrito.setItems(List.of(new ItemCarrito("p", 1, "Grano", 125, 5, null)));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(productos.findById("p")).thenReturn(Optional.of(producto));
		when(carritos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var resultado = service.actualizarProducto("u", 0, 2, "Molido", 250);

		assertEquals(20, resultado.total());
		assertEquals(2, carrito.getItems().get(0).cantidad());
		assertEquals("Molido", carrito.getItems().get(0).molienda());
		assertEquals(10, carrito.getItems().get(0).precioUnitario());
	}

	@Test
	void rechazaActualizacionQueExcedeStock() {
		Producto producto = new Producto("p", "Café", 10, 100);
		Carrito carrito = new Carrito("c", "u");
		carrito.setItems(List.of(new ItemCarrito("p", 1, "Grano", 125, 5, null)));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		assertThrows(IllegalStateException.class, () -> service.actualizarProducto("u", 0, 1, "Grano", 125));
	}

	@Test
	void noPermiteEditarUnaBolsaComoSiFueraUnProducto() {
		Carrito carrito = new Carrito("c", "u");
		carrito.setItems(List.of(new ItemCarrito(null, 1, null, 250, 10, bolsa())));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));

		assertThrows(IllegalArgumentException.class, () -> service.actualizarProducto("u", 0, 1, "Grano", 125));
	}

	@Test
	void rechazaIndicesInexistentes() {
		Carrito carrito = new Carrito("c", "u");
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));

		assertThrows(IllegalArgumentException.class, () -> service.eliminar("u", 0));
		assertThrows(IllegalArgumentException.class, () -> service.actualizarProducto("u", 3, 1, "Grano", 125));
	}

	@Test
	void eliminaYLimpiaCarrito() {
		Carrito carrito = new Carrito("c", "u");
		carrito.setItems(List.of(new ItemCarrito("p1", 1, "Grano", 125, 5, null),
				new ItemCarrito("p2", 1, "Molido", 125, 6, null)));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));

		service.eliminar("u", 0);
		assertEquals(1, carrito.getItems().size());

		service.limpiar("u");
		assertEquals(0, carrito.getItems().size());
		verify(carritos, times(2)).save(carrito);
	}

	@Test
	void agregaBolsaPersonalizadaConCantidadUno() {
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.empty());
		when(carritos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var resultado = service.agregarBolsa("u", bolsa());

		assertEquals(1, resultado.items().size());
		assertTrue(resultado.items().get(0).esBolsa());
		assertEquals(10, resultado.total());
	}

	@Test
	void rechazaBolsaVaciaONula() {
		assertThrows(IllegalArgumentException.class, () -> service.agregarBolsa("u", null));
		assertThrows(IllegalArgumentException.class,
				() -> service.agregarBolsa("u", new BolsaPersonalizada("mediana", 250, List.of(), 10)));
	}

	@Test
	void consultaUnCarritoNuevoVacio() {
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.empty());

		var resultado = service.consultar("u");

		assertEquals(0, resultado.total());
		assertTrue(resultado.items().isEmpty());
	}

	@Test
	void fusionaElCarritoDelVisitanteSumandoLineasIgualesConTopeDe99() {
		Carrito carrito = new Carrito("c", "u");
		carrito.setItems(List.of(new ItemCarrito("p", 2, "Grano", 125, 5, null)));
		Carrito visitante = new Carrito("v", null);
		visitante.setItems(List.of(new ItemCarrito("p", 98, "Grano", 125, 5, null),
				new ItemCarrito("q", 1, "Molido", 250, 10, null)));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));

		service.fusionar("u", visitante);

		assertEquals(2, carrito.getItems().size());
		assertEquals(99, carrito.getItems().get(0).cantidad());
		assertEquals("q", carrito.getItems().get(1).productoId());
		verify(carritos).save(carrito);
	}

	@Test
	void rechazaFusionarUnCarritoNulo() {
		assertThrows(IllegalArgumentException.class, () -> service.fusionar("u", null));
	}
}
