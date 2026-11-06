package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class PedidoAdministrativoServiceTest {

	@Mock
	PedidoRepository pedidos;

	@Mock
	PagoRepository pagos;

	@Mock
	ProductoRepository productos;

	@Mock
	InventarioService inventario;

	@InjectMocks
	PedidoAdministrativoService service;

	private Pedido pedidoEn(EstadoPedido estado) {
		Pedido pedido = new Pedido("ped-1", "u", "Calle 1", List.of(), 20);
		pedido.setEstado(estado);
		return pedido;
	}

	@Test
	void enviaUnPedidoEnProcesoYRegistraLaFechaDeEnvio() {
		Pedido pedido = new Pedido();
		when(pedidos.findById("p")).thenReturn(Optional.of(pedido));

		var resultado = service.actualizarEstado("p", EstadoPedido.ENVIADO);

		assertEquals(EstadoPedido.ENVIADO, pedido.getEstado());
		assertEquals(EstadoPedido.ENVIADO, resultado.estado());
		assertNotNull(pedido.getFechaEnvio());
		verify(pedidos).save(pedido);
	}

	@Test
	void entregaUnPedidoEnviado() {
		Pedido pedido = pedidoEn(EstadoPedido.ENVIADO);
		when(pedidos.findById("ped-1")).thenReturn(Optional.of(pedido));

		service.actualizarEstado("ped-1", EstadoPedido.ENTREGADO);

		assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
		verify(pedidos).save(pedido);
	}

	@ParameterizedTest
	@CsvSource({ "PROCESANDO,PROCESANDO", "PROCESANDO,ENTREGADO", "ENVIADO,PROCESANDO", "ENVIADO,CANCELADO",
			"ENTREGADO,ENVIADO", "ENTREGADO,CANCELADO", "CANCELADO,PROCESANDO", "CANCELADO,ENVIADO" })
	void rechazaTransicionesNoPermitidas(EstadoPedido actual, EstadoPedido nuevo) {
		Pedido pedido = pedidoEn(actual);
		when(pedidos.findById("ped-1")).thenReturn(Optional.of(pedido));

		assertThrows(IllegalStateException.class, () -> service.actualizarEstado("ped-1", nuevo));

		assertEquals(actual, pedido.getEstado());
		verify(pedidos, never()).save(any(Pedido.class));
		verify(inventario, never()).devolver(any(), anyDouble(), any());
	}

	@Test
	void cancelarComoAdminRestauraElStockYReembolsaElPago() {
		Producto producto = new Producto("p", "Café", 10, 500);
		DetallePedido detalle = new DetallePedido("p", "Café", 2, 125, 5, null);
		Pedido pedido = new Pedido("ped-1", "u", "Calle 1", List.of(detalle), 10);
		Pago pago = new Pago("ped-1", 10, EstadoPago.APROBADO, "SIM-1");
		when(pedidos.findById("ped-1")).thenReturn(Optional.of(pedido));
		when(pagos.findByPedidoId("ped-1")).thenReturn(Optional.of(pago));
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		service.actualizarEstado("ped-1", EstadoPedido.CANCELADO);

		assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
		assertEquals(EstadoPago.REEMBOLSADO, pago.getEstado());
		verify(pagos).save(pago);
		verify(inventario).devolver(producto, 250, "ped-1");
		verify(pedidos).save(pedido);
	}

	@Test
	void cancelarUnaBolsaDevuelveElStockDeCadaCompartimento() {
		Producto cafe = new Producto("a", "Café A", 10, 500);
		Producto tostado = new Producto("b", "Café B", 12, 500);
		BolsaPersonalizada bolsa = new BolsaPersonalizada("mediana", 250,
				List.of(new CompartimentoBolsa("a", "Grano", 125), new CompartimentoBolsa("b", "Molido", 125)), 11);
		DetallePedido detalle = new DetallePedido(null, "Bolsa personalizada", 1, 250, 11, bolsa);
		Pedido pedido = new Pedido("ped-1", "u", "Calle 1", List.of(detalle), 11);
		when(pedidos.findById("ped-1")).thenReturn(Optional.of(pedido));
		when(pagos.findByPedidoId("ped-1")).thenReturn(Optional.empty());
		when(productos.findById("a")).thenReturn(Optional.of(cafe));
		when(productos.findById("b")).thenReturn(Optional.of(tostado));

		service.actualizarEstado("ped-1", EstadoPedido.CANCELADO);

		verify(inventario).devolver(cafe, 125, "ped-1");
		verify(inventario).devolver(tostado, 125, "ped-1");
	}

	@Test
	void soloReembolsaPagosAprobados() {
		Pedido pedido = pedidoEn(EstadoPedido.PROCESANDO);
		Pago pago = new Pago("ped-1", 20, EstadoPago.FALLIDO, "SIM-1");
		when(pedidos.findById("ped-1")).thenReturn(Optional.of(pedido));
		when(pagos.findByPedidoId("ped-1")).thenReturn(Optional.of(pago));

		service.actualizarEstado("ped-1", EstadoPedido.CANCELADO);

		assertEquals(EstadoPago.FALLIDO, pago.getEstado());
		verify(pagos, never()).save(any(Pago.class));
	}

	@Test
	void rechazaEstadoNuloOPedidoInexistente() {
		assertThrows(IllegalArgumentException.class, () -> service.actualizarEstado("p", null));

		when(pedidos.findById("x")).thenReturn(Optional.empty());
		assertThrows(IllegalArgumentException.class, () -> service.actualizarEstado("x", EstadoPedido.ENVIADO));
		assertThrows(IllegalArgumentException.class, () -> service.detalle("x"));
	}

	@Test
	void consultaDetalleAdministrativo() {
		Pedido pedido = new Pedido("p", "u", "Calle 1", "Ana", "300", List.of(), 20);
		when(pedidos.findById("p")).thenReturn(Optional.of(pedido));

		var resultado = service.detalle("p");

		assertEquals("p", resultado.id());
		assertEquals(20, resultado.total());
		assertEquals("Ana", resultado.destinatario());
		assertEquals("300", resultado.telefonoEnvio());
		verify(pedidos).findById("p");
	}

	@Test
	void listaTodosLosPedidos() {
		when(pedidos.findAll()).thenReturn(List.of(pedidoEn(EstadoPedido.ENVIADO), pedidoEn(EstadoPedido.CANCELADO)));

		assertEquals(2, service.listar().size());

		verify(pedidos).findAll();
	}

	@Test
	void pasaAEntregadoLosPedidosEnviadosQueYaEsperaronLoSuficiente() {
		Instant ahora = Instant.now();
		Pedido viejo = pedidoEn(EstadoPedido.ENVIADO);
		viejo.setFechaEnvio(ahora.minus(10, ChronoUnit.MINUTES));
		Pedido reciente = pedidoEn(EstadoPedido.ENVIADO);
		reciente.setFechaEnvio(ahora.minus(1, ChronoUnit.MINUTES));
		when(pedidos.findByEstado(EstadoPedido.ENVIADO)).thenReturn(List.of(viejo, reciente));

		int avanzados = service.avanzarEnviadosAEntregado(ahora);

		assertEquals(1, avanzados);
		assertEquals(EstadoPedido.ENTREGADO, viejo.getEstado());
		assertEquals(EstadoPedido.ENVIADO, reciente.getEstado());
		verify(pedidos, times(1)).save(viejo);
		verify(pedidos, never()).save(reciente);
	}

	@Test
	void sinFechaDeEnvioUsaLaFechaDeCreacionDelPedido() {
		Instant ahora = Instant.now();
		Pedido pedido = pedidoEn(EstadoPedido.ENVIADO);
		pedido.setFecha(ahora.minus(1, ChronoUnit.HOURS));
		when(pedidos.findByEstado(EstadoPedido.ENVIADO)).thenReturn(List.of(pedido));

		assertEquals(1, service.avanzarEnviadosAEntregado(ahora));
		assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
	}
}
