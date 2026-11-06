package com.metcoffee.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.metcoffee.config.SeguridadConfig;
import com.metcoffee.security.JwtAuthenticationFilter;
import com.metcoffee.security.JwtService;
import com.metcoffee.service.ProductoService;

@WebMvcTest(ProductoController.class)
@Import({ SeguridadConfig.class, JwtAuthenticationFilter.class })
class ProductoControllerSecurityTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	ProductoService productoService;

	@MockBean
	JwtService jwtService;

	@Test
	void rechazaAccesoAdministrativoSinToken() throws Exception {
		mockMvc.perform(get("/api/admin/productos")).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "ADMINISTRADOR")
	void permiteAccesoAdministrativoAUnAdministrador() throws Exception {
		when(productoService.listar()).thenReturn(List.of());

		mockMvc.perform(get("/api/admin/productos").with(csrf())).andExpect(status().isOk());
	}
}
