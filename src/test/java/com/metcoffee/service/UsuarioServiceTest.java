package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metcoffee.model.Carrito;
import com.metcoffee.model.Direccion;
import com.metcoffee.model.Rol;
import com.metcoffee.model.Usuario;
import com.metcoffee.repository.CarritoRepository;
import com.metcoffee.repository.DireccionRepository;
import com.metcoffee.repository.PedidoRepository;
import com.metcoffee.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	UsuarioRepository usuarios;

	@Mock
	PedidoRepository pedidos;

	@Mock
	CarritoRepository carritos;

	@Mock
	DireccionRepository direcciones;

	@InjectMocks
	UsuarioService service;

	private Usuario ana() {
		return new Usuario("u", "Ana", "a@b", "p", "1", Rol.CLIENTE);
	}

	@Test
	void listaUsuarios() {
		when(usuarios.findAll()).thenReturn(List.of(ana(), new Usuario("a", "Admin", "ad@b", "p", "2", Rol.ADMINISTRADOR)));

		assertEquals(2, service.listar().size());
	}

	@Test
	void consultaUsuario() {
		when(usuarios.findById("u")).thenReturn(Optional.of(ana()));

		assertEquals("Ana", service.consultar("u").nombreUsuario());

		verify(usuarios).findById("u");
	}

	@Test
	void rechazaUsuarioInexistente() {
		when(usuarios.findById("u")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.consultar("u"));
	}

	@Test
	void cambiaElRolDeOtroUsuario() {
		Usuario usuario = ana();
		when(usuarios.findById("u")).thenReturn(Optional.of(usuario));
		when(usuarios.save(usuario)).thenReturn(usuario);

		var resultado = service.cambiarRol("admin", "u", Rol.ADMINISTRADOR);

		assertEquals(Rol.ADMINISTRADOR, resultado.rol());
		verify(usuarios).save(usuario);
	}

	@Test
	void noPermiteCambiarElPropioRolNiUnRolNulo() {
		assertThrows(IllegalStateException.class, () -> service.cambiarRol("u", "u", Rol.CLIENTE));
		assertThrows(IllegalArgumentException.class, () -> service.cambiarRol("admin", "u", null));

		verify(usuarios, never()).save(any());
	}

	@Test
	void eliminaUnUsuarioSinPedidosYLimpiaSusDatos() {
		Usuario usuario = ana();
		Carrito carrito = new Carrito("c", "u");
		List<Direccion> misDirecciones = List.of(new Direccion("d", "u", "Casa", "Ana", "Calle 1", "300", true));
		when(usuarios.findById("u")).thenReturn(Optional.of(usuario));
		when(pedidos.existsByUsuarioId("u")).thenReturn(false);
		when(carritos.findByUsuarioId("u")).thenReturn(Optional.of(carrito));
		when(direcciones.findByUsuarioId("u")).thenReturn(misDirecciones);

		service.eliminar("admin", "u");

		verify(carritos).delete(carrito);
		verify(direcciones).deleteAll(misDirecciones);
		verify(usuarios).delete(usuario);
	}

	@Test
	void noPermiteEliminarseASiMismo() {
		assertThrows(IllegalStateException.class, () -> service.eliminar("u", "u"));

		verify(usuarios, never()).delete(any(Usuario.class));
	}

	@Test
	void noPermiteEliminarUnUsuarioConPedidos() {
		when(usuarios.findById("u")).thenReturn(Optional.of(ana()));
		when(pedidos.existsByUsuarioId("u")).thenReturn(true);

		assertThrows(IllegalStateException.class, () -> service.eliminar("admin", "u"));

		verify(usuarios, never()).delete(any(Usuario.class));
		verify(carritos, never()).delete(any(Carrito.class));
	}

	@Test
	void rechazaEliminarUnUsuarioInexistente() {
		when(usuarios.findById("x")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.eliminar("admin", "x"));
	}
}
