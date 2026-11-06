package com.metcoffee.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.metcoffee.model.Rol;
import com.metcoffee.model.Usuario;
import com.metcoffee.model.Carrito;
import com.metcoffee.model.Direccion;
import com.metcoffee.model.ItemCarrito;
import com.metcoffee.model.Producto;
import com.metcoffee.model.Pedido;
import com.metcoffee.dto.UsuarioDTO;
import com.metcoffee.repository.CarritoRepository;
import com.metcoffee.repository.DireccionRepository;
import com.metcoffee.repository.PagoRepository;
import com.metcoffee.repository.PedidoRepository;
import com.metcoffee.repository.ProductoRepository;
import com.metcoffee.repository.MovimientoInventarioRepository;
import com.metcoffee.repository.UsuarioRepository;
import com.metcoffee.service.CompraService;
import com.metcoffee.service.InventarioService;
import com.metcoffee.service.PasarelaPago;
import com.metcoffee.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"spring.data.mongodb.uri=${MONGODB_TEST_URI:mongodb://127.0.0.1:27018/metcoffee_test?replicaSet=rs0&directConnection=true}"
})
@EnabledIfSystemProperty(named = "integration", matches = "true")
class MongoIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtService jwtService;

	@Autowired
	UsuarioRepository usuarios;

	@MockBean
	PasarelaPago pasarelaPago;

	@Autowired
	CompraService compraService;

	@Autowired
	CarritoRepository carritos;

	@Autowired
	DireccionRepository direcciones;

	@Autowired
	PagoRepository pagos;

	@Autowired
	PedidoRepository pedidos;

	@Autowired
	ProductoRepository productos;

	@Autowired
	MovimientoInventarioRepository movimientos;

	@SpyBean
	InventarioService inventario;

	@Test
	void guardaYLeeUnDocumentoRealDesdeMongo() {
		String id = UUID.randomUUID().toString();
		Usuario guardado = usuarios.save(new Usuario(id, "mongo_test", id + "@test.local", "hash", "000", Rol.CLIENTE));

		assertEquals(id, usuarios.findById(guardado.getId()).orElseThrow().getId());
		usuarios.deleteById(id);
	}

	@Test
	void revierteLaCompraCompletaSiFallaUnDescuento() {
		String usuario = "rollback-" + UUID.randomUUID();
		String direccion = "direccion-" + UUID.randomUUID();
		String productoUno = "producto-" + UUID.randomUUID();
		String productoDos = "producto-" + UUID.randomUUID();
		long pagosAntes = pagos.count();
		productos.save(new Producto(productoUno, "Cafe uno", 10, 500));
		productos.save(new Producto(productoDos, "Cafe dos", 10, 500));
		direcciones.save(new Direccion(direccion, usuario, "Casa", "Ana", "Calle 1", "300", true));
		Carrito carrito = new Carrito("carrito-" + UUID.randomUUID(), usuario);
		carrito.setItems(List.of(new ItemCarrito(productoUno, 1, "Grano", 125, 5, null),
				new ItemCarrito(productoDos, 1, "Grano", 125, 5, null)));
		carritos.save(carrito);
		when(pasarelaPago.procesar(anyString(), anyDouble())).thenReturn(true);
		AtomicInteger descuentos = new AtomicInteger();
		doAnswer(invocation -> {
			if (descuentos.incrementAndGet() == 2) {
				throw new IllegalStateException("Fallo de inventario simulado");
			}
			return invocation.callRealMethod();
		}).when(inventario).descontar(any(Producto.class), anyDouble(), anyString());

		org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
				() -> compraService.confirmar(usuario, direccion));

		assertTrue(pedidos.findByUsuarioIdOrderByFechaDesc(usuario).isEmpty());
		assertEquals(500, productos.findById(productoUno).orElseThrow().getStockGramos());
		assertEquals(500, productos.findById(productoDos).orElseThrow().getStockGramos());
		assertEquals(pagosAntes, pagos.count(), "el pago aprobado también debe revertirse");
		assertTrue(movimientos.findByProductoIdOrderByFechaDesc(productoUno).isEmpty());
		assertTrue(movimientos.findByProductoIdOrderByFechaDesc(productoDos).isEmpty());
		assertEquals(2, carritos.findByUsuarioId(usuario).orElseThrow().getItems().size());
	}

	@Test
	void deniegaConsultarPedidoDeOtroUsuarioYPermiteAlPropietario() throws Exception {
		String usuarioA = "cliente-a-" + UUID.randomUUID();
		String usuarioB = "cliente-b-" + UUID.randomUUID();
		String pedidoId = "pedido-" + UUID.randomUUID();
		usuarios.save(new Usuario(usuarioA, "cliente_a", usuarioA + "@test.local", "hash", "000", Rol.CLIENTE));
		usuarios.save(new Usuario(usuarioB, "cliente_b", usuarioB + "@test.local", "hash", "000", Rol.CLIENTE));
		pedidos.save(new Pedido(pedidoId, usuarioB, "Calle de prueba", List.of(), 0));

		String tokenA = jwtService.generateToken(new UsuarioDTO(usuarioA, "cliente_a", usuarioA + "@test.local", "000",
				Rol.CLIENTE));
		String tokenB = jwtService.generateToken(new UsuarioDTO(usuarioB, "cliente_b", usuarioB + "@test.local", "000",
				Rol.CLIENTE));

		try {
			mockMvc.perform(get("/api/pedidos/{usuarioId}/{id}", usuarioB, pedidoId)
					.header("Authorization", "Bearer " + tokenA))
				.andExpect(status().isForbidden());
			mockMvc.perform(get("/api/pedidos/{usuarioId}/{id}", usuarioB, pedidoId)
					.header("Authorization", "Bearer " + tokenB))
				.andExpect(status().isOk());
		} finally {
			pedidos.deleteById(pedidoId);
			usuarios.deleteById(usuarioA);
			usuarios.deleteById(usuarioB);
		}
	}
}
