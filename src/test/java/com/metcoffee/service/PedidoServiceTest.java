package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metcoffee.model.BolsaPersonalizada;
import com.metcoffee.model.CompartimentoBolsa;
import com.metcoffee.model.DetallePedido;
import com.metcoffee.model.EstadoPago;
import com.metcoffee.model.EstadoPedido;
import com.metcoffee.model.Pago;
import com.metcoffee.model.Pedido;
import com.metcoffee.model.Producto;
import com.metcoffee.repository.PagoRepository;
import com.metcoffee.repository.PedidoRepository;
import com.metcoffee.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

	@Mock
	PedidoRepository pedidos;

	@Mock
	PagoRepository pagos;

	@Mock
	ProductoRepository productos;

	@Mock
	InventarioService inventario;

	@InjectMocks
	PedidoService service;

	@Test
	void historialVacio() {
		when(pedidos.findByUsuarioIdOrderByFechaDesc("u")).thenReturn(List.of());

		assertTrue(service.historial("u").isEmpty());

		verify(pedidos).findByUsuarioIdOrderByFechaDesc("u");
	}

	@Test
	void cancelaPedidoEnProcesoYReembolsaPago() {
		Producto producto = new Producto("p", "Café", 10, 500);
		DetallePedido detalle = new DetallePedido("p", "Café", 2, 125, 10, null);
		Pedido pedido = new Pedido("pedido-1", "u", "Calle 1", List.of(detalle), 20);
		Pago pago = new Pago("pedido-1", 20, EstadoPago.APROBADO, "SIM-pedido-1");

		when(pedidos.findById("pedido-1")).thenReturn(Optional.of(pedido));
		when(pagos.findByPedidoId("pedido-1")).thenReturn(Optional.of(pago));
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		service.cancelar("u", "pedido-1");

		assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
		assertEquals(EstadoPago.REEMBOLSADO, pago.getEstado());
		verify(pedidos).save(pedido);
		verify(pagos).save(pago);
		verify(inventario).devolver(producto, 250, "pedido-1");
	}

	@Test
	void cancelaPedidoConBolsaPersonalizadaYDevuelveStockPorCompartimentos() {
		Producto cafe = new Producto("p", "Café", 10, 500);
		Producto tostado = new Producto("t", "Tostado", 12, 500);
		BolsaPersonalizada bolsa = new BolsaPersonalizada(
				"mediana",
				250,
				List.of(
						new CompartimentoBolsa("p", "Grano", 125),
						new CompartimentoBolsa("t", "Molido", 125)),
				15);
		DetallePedido detalle = new DetallePedido(null, "Bolsa personalizada", 1, 250, 15, bolsa);
		Pedido pedido = new Pedido("pedido-1", "u", "Calle 1", List.of(detalle), 15);
		Pago pago = new Pago("pedido-1", 15, EstadoPago.APROBADO, "SIM-pedido-1");

		when(pedidos.findById("pedido-1")).thenReturn(Optional.of(pedido));
		when(pagos.findByPedidoId("pedido-1")).thenReturn(Optional.of(pago));
		when(productos.findById("p")).thenReturn(Optional.of(cafe));
		when(productos.findById("t")).thenReturn(Optional.of(tostado));

		service.cancelar("u", "pedido-1");

		assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
		verify(inventario, times(2)).devolver(any(Producto.class), any(Double.class), eq("pedido-1"));
		verify(pagos).save(pago);
	}

	@Test
	void soloReembolsaPagosAprobados() {
		Producto producto = new Producto("p", "Café", 10, 500);
		DetallePedido detalle = new DetallePedido("p", "Café", 2, 125, 10, null);
		Pedido pedido = new Pedido("pedido-1", "u", "Calle 1", List.of(detalle), 20);
		Pago pago = new Pago("pedido-1", 20, EstadoPago.FALLIDO, "SIM-pedido-1");

		when(pedidos.findById("pedido-1")).thenReturn(Optional.of(pedido));
		when(pagos.findByPedidoId("pedido-1")).thenReturn(Optional.of(pago));
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		service.cancelar("u", "pedido-1");

		assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
		assertEquals(EstadoPago.FALLIDO, pago.getEstado());
		verify(pagos, never()).save(any(Pago.class));
		verify(inventario).devolver(producto, 250, "pedido-1");
	}

	@Test
	void noCancelaPedidoYaEnviado() {
		Pedido pedido = new Pedido("pedido-1", "u", "Calle 1", List.of(), 20);
		pedido.setEstado(EstadoPedido.ENVIADO);
		when(pedidos.findById("pedido-1")).thenReturn(Optional.of(pedido));

		assertThrows(IllegalStateException.class, () -> service.cancelar("u", "pedido-1"));

		verify(pedidos, never()).save(any(Pedido.class));
		verify(pagos, never()).save(any(Pago.class));
		verify(inventario, never()).devolver(any(), any(Double.class), any());
	}

	@Test
	void detalleIncluyeLosDatosDeEnvio() {
		Pedido pedido = new Pedido("pedido-1", "u", "Calle 1", "Ana", "300", List.of(), 20);
		when(pedidos.findById("pedido-1")).thenReturn(Optional.of(pedido));

		var resultado = service.detalle("u", "pedido-1");

		assertEquals("Calle 1", resultado.direccionEnvio());
		assertEquals("Ana", resultado.destinatario());
		assertEquals("300", resultado.telefonoEnvio());
	}

	@Test
	void noMuestraNiCancelaPedidosDeOtroUsuario() {
		Pedido pedido = new Pedido("pedido-1", "otro", "Calle 1", List.of(), 20);
		when(pedidos.findById("pedido-1")).thenReturn(Optional.of(pedido));

		assertThrows(IllegalArgumentException.class, () -> service.detalle("u", "pedido-1"));
		assertThrows(IllegalArgumentException.class, () -> service.cancelar("u", "pedido-1"));

		verify(pedidos, never()).save(any(Pedido.class));
	}

	@Test
	void rechazaPedidoInexistente() {
		when(pedidos.findById("x")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.detalle("u", "x"));
	}
}
