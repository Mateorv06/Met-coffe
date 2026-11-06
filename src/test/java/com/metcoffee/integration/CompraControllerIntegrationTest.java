package com.metcoffee.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metcoffee.dto.ConfirmarCompraRequest;
import com.metcoffee.dto.UsuarioDTO;
import com.metcoffee.model.Carrito;
import com.metcoffee.model.Direccion;
import com.metcoffee.model.ItemCarrito;
import com.metcoffee.model.Pedido;
import com.metcoffee.model.Producto;
import com.metcoffee.model.Rol;
import com.metcoffee.model.Usuario;
import com.metcoffee.repository.CarritoRepository;
import com.metcoffee.repository.DireccionRepository;
import com.metcoffee.repository.MovimientoInventarioRepository;
import com.metcoffee.repository.PagoRepository;
import com.metcoffee.repository.PedidoRepository;
import com.metcoffee.repository.ProductoRepository;
import com.metcoffee.repository.UsuarioRepository;
import com.metcoffee.security.JwtService;

/**
 * Comprueba el flujo HTTP real de compra hasta las colecciones de MongoDB.
 * La pasarela simulada real del proyecto aprueba el pago de prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=${MONGODB_TEST_URI:mongodb://127.0.0.1:27018/metcoffee_test?replicaSet=rs0&directConnection=true}"
})
@EnabledIfSystemProperty(named = "integration", matches = "true")
class CompraControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ProductoRepository productos;

    @Autowired
    private CarritoRepository carritos;

    @Autowired
    private DireccionRepository direcciones;

    @Autowired
    private PedidoRepository pedidos;

    @Autowired
    private PagoRepository pagos;

    @Autowired
    private MovimientoInventarioRepository movimientos;

    @Test
    void confirmaCompraDesdeHttpYActualizaPedidoPagoCarritoEInventario() throws Exception {
        String usuarioId = "compra-integracion-" + UUID.randomUUID();
        String productoId = "producto-integracion-" + UUID.randomUUID();
        String direccionId = "direccion-integracion-" + UUID.randomUUID();
        String carritoId = "carrito-integracion-" + UUID.randomUUID();
        String pedidoId = null;

        try {
            usuarios.save(new Usuario(usuarioId, "cliente_integracion", usuarioId + "@test.local",
                    "hash-solo-pruebas", "3000000000", Rol.CLIENTE));
            productos.save(new Producto(productoId, "Cafe Compra Integracion", 20000, 500));
            direcciones.save(new Direccion(direccionId, usuarioId, "Casa", "Cliente Integracion",
                    "Calle de prueba 123", "3000000000", true));

            Carrito carrito = new Carrito(carritoId, usuarioId);
            // 125 g cuestan la mitad del precio base (que corresponde a 250 g).
            carrito.setItems(List.of(new ItemCarrito(productoId, 1, "Grano", 125, 10000, null)));
            carritos.save(carrito);

            String tokenCliente = jwtService.generateToken(new UsuarioDTO(usuarioId, "cliente_integracion",
                    usuarioId + "@test.local", "3000000000", Rol.CLIENTE));

            MvcResult resultado = mockMvc.perform(post("/api/compras/{usuarioId}", usuarioId)
                    .header("Authorization", "Bearer " + tokenCliente)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ConfirmarCompraRequest(direccionId))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.total").value(10000.0))
                    .andExpect(jsonPath("$.items[0].nombreProducto").value("Cafe Compra Integracion"))
                    .andReturn();

            pedidoId = objectMapper.readTree(resultado.getResponse().getContentAsString())
                    .get("id").asText();

            Pedido pedidoGuardado = pedidos.findById(pedidoId).orElseThrow();
            org.junit.jupiter.api.Assertions.assertEquals(usuarioId, pedidoGuardado.getUsuarioId());
            org.junit.jupiter.api.Assertions.assertEquals(10000.0, pedidoGuardado.getTotal(), 0.001);

            org.junit.jupiter.api.Assertions.assertEquals(375.0,
                    productos.findById(productoId).orElseThrow().getStockGramos(), 0.001);
            org.junit.jupiter.api.Assertions.assertTrue(carritos.findByUsuarioId(usuarioId)
                    .orElseThrow().getItems().isEmpty());
            org.junit.jupiter.api.Assertions.assertTrue(pagos.findByPedidoId(pedidoId).isPresent());
            org.junit.jupiter.api.Assertions.assertEquals(1,
                    movimientos.findByProductoIdOrderByFechaDesc(productoId).size());
        } finally {
            if (pedidoId != null) {
                pagos.findByPedidoId(pedidoId).ifPresent(pagos::delete);
                pedidos.deleteById(pedidoId);
            }
            movimientos.deleteAll(movimientos.findByProductoIdOrderByFechaDesc(productoId));
            carritos.findByUsuarioId(usuarioId).ifPresent(carritos::delete);
            direcciones.findById(direccionId).ifPresent(direcciones::delete);
            productos.findById(productoId).ifPresent(productos::delete);
            usuarios.findById(usuarioId).ifPresent(usuarios::delete);
        }
    }

    @Test
    void rechazaConfirmarCompraSinAutenticacion() throws Exception {
        mockMvc.perform(post("/api/compras/{usuarioId}", "usuario-sin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmarCompraRequest("direccion-no-usada"))))
                .andExpect(status().isUnauthorized());
    }
}
