package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metcoffee.model.EstadoPedido;
import com.metcoffee.model.Pedido;
import com.metcoffee.model.Producto;
import com.metcoffee.model.Rol;
import com.metcoffee.repository.PedidoRepository;
import com.metcoffee.repository.ProductoRepository;
import com.metcoffee.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	@Mock
	PedidoRepository pedidos;

	@Mock
	UsuarioRepository usuarios;

	@Mock
	ProductoRepository productos;

	@InjectMocks
	DashboardService service;

	private Pedido pedido(String id, double total, EstadoPedido estado) {
		Pedido pedido = new Pedido(id, "u", "Calle 1", List.of(), total);
		pedido.setEstado(estado);
		return pedido;
	}

	@Test
	void dashboardVacio() {
		when(pedidos.findAll()).thenReturn(List.of());
		when(usuarios.countByRol(Rol.CLIENTE)).thenReturn(0L);
		when(productos.findByActivoTrue()).thenReturn(List.of());

		var resultado = service.consultar(10);

		assertEquals(0, resultado.pedidos());
		assertEquals(0, resultado.ingresos());
		verify(pedidos).findAll();
	}

	@Test
	void losIngresosSoloSumanPedidosEntregados() {
		when(pedidos.findAll()).thenReturn(List.of(
				pedido("1", 100, EstadoPedido.ENTREGADO),
				pedido("2", 50, EstadoPedido.ENVIADO),
				pedido("3", 30, EstadoPedido.CANCELADO),
				pedido("4", 20, EstadoPedido.PROCESANDO),
				pedido("5", 400, EstadoPedido.ENTREGADO)));
		when(usuarios.countByRol(Rol.CLIENTE)).thenReturn(0L);
		when(productos.findByActivoTrue()).thenReturn(List.of());

		var resultado = service.consultar(10);

		assertEquals(500, resultado.ingresos());
		assertEquals(5, resultado.pedidos());
	}

	@Test
	void cuentaSoloLosUsuariosConRolCliente() {
		when(pedidos.findAll()).thenReturn(List.of());
		when(usuarios.countByRol(Rol.CLIENTE)).thenReturn(2L);
		when(productos.findByActivoTrue()).thenReturn(List.of());

		var resultado = service.consultar(10);

		assertEquals(2, resultado.clientes());
		verify(usuarios, never()).count();
	}

	@Test
	void identificaProductosConBajoStock() {
		Producto bajo = new Producto("p", "Café", 10, 5);
		Producto suficiente = new Producto("q", "Otro", 10, 5000);
		when(pedidos.findAll()).thenReturn(List.of());
		when(usuarios.countByRol(Rol.CLIENTE)).thenReturn(2L);
		when(productos.findByActivoTrue()).thenReturn(List.of(bajo, suficiente));

		var resultado = service.consultar(10);

		assertEquals(1, resultado.bajoStock().size());
		assertEquals("p", resultado.bajoStock().get(0).getId());
	}
}
