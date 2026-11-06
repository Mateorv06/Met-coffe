package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metcoffee.model.MovimientoInventario;
import com.metcoffee.model.Producto;
import com.metcoffee.model.TipoMovimiento;
import com.metcoffee.repository.MovimientoInventarioRepository;
import com.metcoffee.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

	@Mock
	ProductoRepository productos;

	@Mock
	MovimientoInventarioRepository movimientos;

	@InjectMocks
	InventarioService service;

	private MovimientoInventario movimientoGuardado() {
		ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
		verify(movimientos).save(captor.capture());
		return captor.getValue();
	}

	@Test
	void ajustaStockYRegistraLaDiferencia() {
		Producto producto = new Producto("p", "Café", 1, 10);
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		service.ajustar("p", 20, "prueba");

		assertEquals(20, producto.getStockGramos());
		MovimientoInventario movimiento = movimientoGuardado();
		assertEquals(TipoMovimiento.AJUSTE_MANUAL, movimiento.tipo());
		assertEquals(10, movimiento.gramos());
		assertEquals("prueba", movimiento.nota());
	}

	@Test
	void rechazaStockInvalidoSinGuardarNada() {
		assertThrows(IllegalArgumentException.class, () -> service.ajustar("p", -1, "x"));
		assertThrows(IllegalArgumentException.class, () -> service.ajustar("p", Double.NaN, "x"));

		verify(productos, never()).save(any());
		verify(movimientos, never()).save(any());
	}

	@Test
	void rechazaAjustarUnProductoInexistente() {
		when(productos.findById("x")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.ajustar("x", 5, "x"));

		verify(movimientos, never()).save(any());
	}

	@Test
	void consultaStockDelProducto() {
		Producto producto = new Producto("p", "Café", 1, 20);
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		assertEquals(20, service.consultar("p").getStockGramos());

		verify(productos).findById("p");
	}

	@Test
	void descuentaStockYRegistraUnaVenta() {
		Producto producto = new Producto("p", "Café", 1, 500);

		service.descontar(producto, 250, "pedido-1");

		assertEquals(250, producto.getStockGramos());
		verify(productos).save(producto);
		MovimientoInventario movimiento = movimientoGuardado();
		assertEquals(TipoMovimiento.VENTA, movimiento.tipo());
		assertEquals(-250, movimiento.gramos());
		assertEquals("pedido-1", movimiento.pedidoId());
	}

	@Test
	void rechazaDescuentosConGramosNoPositivos() {
		Producto producto = new Producto("p", "Café", 1, 20);

		assertThrows(IllegalArgumentException.class, () -> service.descontar(producto, 0, "pedido-1"));
		assertThrows(IllegalArgumentException.class, () -> service.descontar(producto, -10, "pedido-1"));
		verify(productos, never()).save(any());
		verify(movimientos, never()).save(any());
	}

	@Test
	void rechazaDescontarMasDeLoQueHay() {
		Producto producto = new Producto("p", "Café", 1, 100);

		assertThrows(IllegalStateException.class, () -> service.descontar(producto, 150, "pedido-1"));

		assertEquals(100, producto.getStockGramos());
		verify(movimientos, never()).save(any());
	}

	@Test
	void devuelveStockYRegistraLaDevolucionPorCancelacion() {
		Producto producto = new Producto("p", "Café", 1, 100);

		service.devolver(producto, 250, "pedido-1");

		assertEquals(350, producto.getStockGramos());
		MovimientoInventario movimiento = movimientoGuardado();
		assertEquals(TipoMovimiento.DEVOLUCION_CANCELACION, movimiento.tipo());
		assertEquals(250, movimiento.gramos());
	}

	@Test
	void rechazaProductoNuloAlDescontarODevolver() {
		assertThrows(IllegalArgumentException.class, () -> service.descontar(null, 10, "pedido-1"));
		assertThrows(IllegalArgumentException.class, () -> service.devolver(null, 10, "pedido-1"));
	}

	@Test
	void listaTodosLosMovimientos() {
		MovimientoInventario movimiento = new MovimientoInventario("m1", "p", null, TipoMovimiento.CARGA_INICIAL, 500,
				Instant.now(), "inicial");
		when(movimientos.findAllByOrderByFechaDesc()).thenReturn(List.of(movimiento));

		assertEquals(1, service.listarMovimientos().size());

		verify(movimientos).findAllByOrderByFechaDesc();
	}

	@Test
	void listaLosMovimientosDeUnProducto() {
		MovimientoInventario movimiento = new MovimientoInventario("m1", "p", "pedido-1", TipoMovimiento.VENTA, -250,
				Instant.now(), "Compra confirmada");
		when(productos.findById("p")).thenReturn(Optional.of(new Producto("p", "Café", 1, 100)));
		when(movimientos.findByProductoIdOrderByFechaDesc("p")).thenReturn(List.of(movimiento));

		var resultado = service.listarMovimientos("p");

		assertEquals(1, resultado.size());
		assertEquals(TipoMovimiento.VENTA, resultado.get(0).tipo());
	}

	@Test
	void rechazaListarMovimientosDeUnProductoInexistente() {
		when(productos.findById("x")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.listarMovimientos("x"));

		verify(movimientos, never()).findByProductoIdOrderByFechaDesc(any());
	}
}
