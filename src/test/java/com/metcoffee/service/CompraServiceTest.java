package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metcoffee.model.BolsaPersonalizada;
import com.metcoffee.model.Carrito;
import com.metcoffee.model.CompartimentoBolsa;
import com.metcoffee.model.Direccion;
import com.metcoffee.model.EstadoPago;
import com.metcoffee.model.EstadoPedido;
import com.metcoffee.model.ItemCarrito;
import com.metcoffee.model.Pago;
import com.metcoffee.model.Pedido;
import com.metcoffee.model.Producto;
import com.metcoffee.repository.CarritoRepository;
import com.metcoffee.repository.DireccionRepository;
import com.metcoffee.repository.PagoRepository;
import com.metcoffee.repository.PedidoRepository;
import com.metcoffee.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

	@Mock
	CarritoRepository carritos;

	@Mock
	DireccionRepository direcciones;

	@Mock
	ProductoRepository productos;

	@Mock
	PedidoRepository pedidos;

	@Mock
	PagoRepository pagos;

	@Mock
	InventarioService inventario;

	@Mock
	PasarelaPago pasarela;

	@InjectMocks
	CompraService service;

	private final Direccion direccion = new Direccion("d", "u", "Casa", "Ana", "Calle 1", "300", true);

	private Carrito carritoCon(ItemCarrito... items) {
		Carrito carrito = new Carrito("c", "u");
		carrito.setItems(List.of(items));
		return carrito;
	}

	@Test
	void rechazaCarritoVacio() {
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> service.confirmar("u", "d"));

		verify(carritos).findByUsuarioId("u");
	}

	@Test
	void confirmaCompraYActualizaSusColaboradores() {
		Producto producto = new Producto("p", "Café", 10, 500);
		Carrito carrito = carritoCon(new ItemCarrito("p", 2, "Grano", 125, 5, null));
		Pedido pedido = new Pedido("pedido-1", "u", "Calle 1", "Ana", "300", List.of(), 10);
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(direcciones.findByIdAndUsuarioId("d", "u")).thenReturn(Optional.of(direccion));
		when(productos.findById("p")).thenReturn(Optional.of(producto));
		when(pasarela.procesar(anyString(), anyDouble())).thenReturn(true);
		when(pedidos.save(any(Pedido.class))).thenReturn(pedido);

		var resultado = service.confirmar("u", "d");

		assertEquals("pedido-1", resultado.id());
		assertEquals(10, resultado.total());
		assertEquals(EstadoPedido.PROCESANDO, resultado.estado());
		assertEquals("Calle 1", resultado.direccionEnvio());
		assertEquals("Ana", resultado.destinatario());
		assertEquals("300", resultado.telefonoEnvio());
		verify(inventario).descontar(producto, 250, "pedido-1");
		verify(carritos).save(carrito);
		assertEquals(0, carrito.getItems().size());

		ArgumentCaptor<Pedido> pedidoGuardado = ArgumentCaptor.forClass(Pedido.class);
		verify(pedidos).save(pedidoGuardado.capture());
		assertEquals("Ana", pedidoGuardado.getValue().getDestinatario());
		assertEquals("300", pedidoGuardado.getValue().getTelefonoEnvio());

		ArgumentCaptor<Pago> pagoGuardado = ArgumentCaptor.forClass(Pago.class);
		verify(pagos).save(pagoGuardado.capture());
		assertEquals(EstadoPago.APROBADO, pagoGuardado.getValue().getEstado());
		assertEquals("pedido-1", pagoGuardado.getValue().getPedidoId());
		assertEquals(10, pagoGuardado.getValue().getMonto());
	}

	@Test
	void rechazaDomicilioDeOtroUsuarioSinCrearPedido() {
		Carrito carrito = carritoCon(new ItemCarrito("p", 1, "Grano", 125, 5, null));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(direcciones.findByIdAndUsuarioId("d", "u")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.confirmar("u", "d"));

		verify(pedidos, never()).save(any(Pedido.class));
		verify(pagos, never()).save(any());
		verify(inventario, never()).descontar(any(), anyDouble(), any());
	}

	@Test
	void descuentaCadaCompartimentoDeUnaBolsaPersonalizada() {
		Producto producto = new Producto("p", "Café", 10, 500);
		BolsaPersonalizada bolsa = new BolsaPersonalizada("mediana", 250,
				List.of(new CompartimentoBolsa("p", "Grano", 125), new CompartimentoBolsa("p", "Molido", 125)), 10);
		Carrito carrito = carritoCon(new ItemCarrito(null, 1, null, 250, 10, bolsa));
		Pedido pedido = new Pedido("pedido-1", "u", "Calle 1", List.of(), 10);
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(direcciones.findByIdAndUsuarioId("d", "u")).thenReturn(Optional.of(direccion));
		when(productos.findById("p")).thenReturn(Optional.of(producto));
		when(pasarela.procesar(anyString(), anyDouble())).thenReturn(true);
		when(pedidos.save(any(Pedido.class))).thenReturn(pedido);

		service.confirmar("u", "d");

		verify(inventario, times(2)).descontar(producto, 125, "pedido-1");
	}

	@Test
	void validaElPrecioDeLaBolsaConElFactorDelTamano() {
		Producto producto = new Producto("p", "Café", 10, 5000);
		BolsaPersonalizada bolsa = new BolsaPersonalizada("grande", 500,
				List.of(new CompartimentoBolsa("p", "Grano", 250), new CompartimentoBolsa("p", "Molido", 250)), 10);
		Carrito carrito = carritoCon(new ItemCarrito(null, 1, null, 500, 10, bolsa)); // debería costar 18
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(direcciones.findByIdAndUsuarioId("d", "u")).thenReturn(Optional.of(direccion));
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		assertThrows(IllegalStateException.class, () -> service.confirmar("u", "d"));

		verify(pedidos, never()).save(any(Pedido.class));
	}

	@Test
	void rechazaPreciosManipulados() {
		Producto producto = new Producto("p", "Café", 10, 500);
		Carrito carrito = carritoCon(new ItemCarrito("p", 1, "Grano", 125, 1, null)); // 125 g debería costar 5
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(direcciones.findByIdAndUsuarioId("d", "u")).thenReturn(Optional.of(direccion));
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		var ex = assertThrows(IllegalStateException.class, () -> service.confirmar("u", "d"));

		assertEquals("Precio inválido", ex.getMessage());
		verify(pedidos, never()).save(any(Pedido.class));
	}

	@Test
	void rechazaStockInsuficienteSumandoLasLineasDelMismoProducto() {
		Producto producto = new Producto("p", "Café", 10, 300);
		Carrito carrito = carritoCon(new ItemCarrito("p", 2, "Grano", 125, 5, null),
				new ItemCarrito("p", 1, "Molido", 125, 5, null)); // 375 g pedidos, 300 g en stock
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(direcciones.findByIdAndUsuarioId("d", "u")).thenReturn(Optional.of(direccion));
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		var ex = assertThrows(IllegalStateException.class, () -> service.confirmar("u", "d"));

		assertEquals("Stock insuficiente", ex.getMessage());
		verify(pedidos, never()).save(any(Pedido.class));
		verify(pagos, never()).save(any());
		verify(inventario, never()).descontar(any(), anyDouble(), any());
	}

	@Test
	void rechazaProductosInactivos() {
		Producto inactivo = new Producto("p", "Café", 10, 500);
		inactivo.setActivo(false);
		Carrito carrito = carritoCon(new ItemCarrito("p", 1, "Grano", 125, 5, null));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(direcciones.findByIdAndUsuarioId("d", "u")).thenReturn(Optional.of(direccion));
		when(productos.findById("p")).thenReturn(Optional.of(inactivo));

		assertThrows(IllegalArgumentException.class, () -> service.confirmar("u", "d"));
	}

	@Test
	void siElPagoSimuladoFallaNoConfirmaLaCompraNiTocaElInventario() {
		Producto producto = new Producto("p", "Café", 10, 500);
		Carrito carrito = carritoCon(new ItemCarrito("p", 2, "Grano", 125, 5, null));
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(direcciones.findByIdAndUsuarioId("d", "u")).thenReturn(Optional.of(direccion));
		when(productos.findById("p")).thenReturn(Optional.of(producto));
		when(pasarela.procesar(anyString(), anyDouble())).thenReturn(false);

		assertThrows(IllegalStateException.class, () -> service.confirmar("u", "d"));

		ArgumentCaptor<Pago> pago = ArgumentCaptor.forClass(Pago.class);
		verify(pagos).save(pago.capture());
		assertEquals(EstadoPago.FALLIDO, pago.getValue().getEstado());
		assertNull(pago.getValue().getPedidoId());
		assertEquals(10, pago.getValue().getMonto());
		verify(pedidos, never()).save(any(Pedido.class));
		verify(inventario, never()).descontar(any(), anyDouble(), any());
		verify(carritos, never()).save(any(Carrito.class));
		assertEquals(1, carrito.getItems().size());
	}
}
