package com.metcoffee.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.metcoffee.dto.ProductoDTO;
import com.metcoffee.dto.UsuarioDTO;
import com.metcoffee.model.Producto;
import com.metcoffee.model.Rol;
import com.metcoffee.repository.ProductoRepository;
import com.metcoffee.security.JwtService;

/**
 * Pruebas de integración HTTP para el CRUD de productos.
 * Se usan el controlador, el servicio y el repositorio reales contra MongoDB de pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=${MONGODB_TEST_URI:mongodb://127.0.0.1:27018/metcoffee_test?replicaSet=rs0&directConnection=true}"
})
@EnabledIfSystemProperty(named = "integration", matches = "true")
class ProductoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoRepository productos;

    @Autowired
    private JwtService jwtService;

    @Test
    void creaConsultaActualizaYDesactivaProductoDesdeElControlador() throws Exception {
        String nombreInicial = "Cafe Integracion " + System.nanoTime();
        String tokenAdmin = tokenAdministrador();
        String productoId = null;

        try {
            ProductoDTO nuevo = new ProductoDTO(null, nombreInicial, "Colombia", "Chocolate", null,
                    28000, 500, true, true);

            MvcResult creacion = mockMvc.perform(post("/api/admin/productos")
                    .header("Authorization", "Bearer " + tokenAdmin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(nuevo)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value(nombreInicial))
                    .andExpect(jsonPath("$.activo").value(true))
                    .andReturn();

            productoId = objectMapper.readTree(creacion.getResponse().getContentAsString())
                    .get("id").asText();

            mockMvc.perform(get("/api/admin/productos")
                    .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(productoId)));

            // La consulta pública permite comprobar el resultado serializado del producto persistido.
            mockMvc.perform(get("/api/catalogo/{id}", productoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(productoId))
                    .andExpect(jsonPath("$.nombre").value(nombreInicial));

            ProductoDTO actualizado = new ProductoDTO(productoId, nombreInicial + " Actualizado", "Colombia",
                    "Chocolate y frutos rojos", null, 29000, 450, false, true);

            mockMvc.perform(put("/api/admin/productos/{id}", productoId)
                    .header("Authorization", "Bearer " + tokenAdmin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(actualizado)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value(nombreInicial + " Actualizado"))
                    .andExpect(jsonPath("$.precioBase").value(29000.0));

            Producto persistido = productos.findById(productoId).orElseThrow();
            org.junit.jupiter.api.Assertions.assertEquals(nombreInicial + " Actualizado", persistido.getNombre());
            org.junit.jupiter.api.Assertions.assertEquals(450.0, persistido.getStockGramos(), 0.001);

            mockMvc.perform(delete("/api/admin/productos/{id}", productoId)
                    .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isNoContent());

            org.junit.jupiter.api.Assertions.assertFalse(productos.findById(productoId).orElseThrow().isActivo());
        } finally {
            if (productoId != null) {
                productos.deleteById(productoId);
            }
        }
    }

    @Test
    void rechazaCrearProductoSinAutorizacionYConDatosInvalidos() throws Exception {
        ProductoDTO invalido = new ProductoDTO(null, "", "Colombia", "Chocolate", null,
                28000, 500, false, true);
        String contenido = objectMapper.writeValueAsString(invalido);

        mockMvc.perform(post("/api/admin/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(contenido))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/productos")
                .header("Authorization", "Bearer " + tokenAdministrador())
                .contentType(MediaType.APPLICATION_JSON)
                .content(contenido))
                .andExpect(status().isBadRequest());
    }

    private String tokenAdministrador() {
        UsuarioDTO administrador = new UsuarioDTO("integracion-admin", "admin_integracion",
                "admin-integracion@test.local", "3000000000", Rol.ADMINISTRADOR);
        return jwtService.generateToken(administrador);
    }
}
