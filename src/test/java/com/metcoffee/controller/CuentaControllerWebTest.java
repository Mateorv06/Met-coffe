package com.metcoffee.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.metcoffee.config.SeguridadConfig;
import com.metcoffee.dto.UsuarioDTO;
import com.metcoffee.model.Rol;
import com.metcoffee.security.JwtAuthenticationFilter;
import com.metcoffee.security.JwtService;
import com.metcoffee.service.CuentaService;
import com.metcoffee.service.UsuarioAutorizacionService;

@WebMvcTest(CuentaController.class)
@Import({ SeguridadConfig.class, JwtAuthenticationFilter.class })
class CuentaControllerWebTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	CuentaService cuentaService;

	@MockBean
	JwtService jwtService;

	@MockBean
	UsuarioAutorizacionService autorizacion;

	@Test
	void loginDevuelveTokenYUsuarioPublico() throws Exception {
		UsuarioDTO usuario = new UsuarioDTO("u1", "ana", "ana@correo.com", "300", Rol.CLIENTE);
		when(cuentaService.iniciarSesion("ana@correo.com", "Clave1234")).thenReturn(usuario);
		when(jwtService.generateToken(usuario)).thenReturn("token-prueba");

		mockMvc.perform(post("/api/cuenta/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"ana@correo.com\",\"password\":\"Clave1234\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("token-prueba"))
				.andExpect(jsonPath("$.usuario.correo").value("ana@correo.com"));
	}
}
