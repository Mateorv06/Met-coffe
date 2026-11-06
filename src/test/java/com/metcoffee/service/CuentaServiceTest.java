package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.metcoffee.model.Carrito;
import com.metcoffee.model.Rol;
import com.metcoffee.model.Usuario;
import com.metcoffee.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

	@Mock
	UsuarioRepository usuarios;

	@Mock
	CarritoService carritos;

	@Mock
	PasswordEncoder encoder;

	@InjectMocks
	CuentaService service;

	private Usuario ana() {
		return new Usuario("u1", "ana", "a@b.co", "hash", "1", Rol.CLIENTE);
	}

	@Test
	void registraCuentaGuardandoLaContrasenaCifradaYElCorreoNormalizado() {
		when(usuarios.findByCorreo("a@b.co")).thenReturn(Optional.empty());
		when(usuarios.findByNombreUsuario("ana")).thenReturn(Optional.empty());
		when(encoder.encode("Clave1234")).thenReturn("hash");
		when(usuarios.save(any())).thenAnswer(invocation -> {
			Usuario usuario = invocation.getArgument(0);
			usuario.setId("u1");
			return usuario;
		});

		var resultado = service.registrar("ana", "A@B.co", "Clave1234", "1");

		assertEquals("u1", resultado.id());
		assertEquals(Rol.CLIENTE, resultado.rol());
		ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarios).save(guardado.capture());
		assertEquals("hash", guardado.getValue().getPassword());
		assertNotEquals("Clave1234", guardado.getValue().getPassword());
		assertEquals("a@b.co", guardado.getValue().getCorreo());
	}

	@Test
	void aceptaContrasenasEntre8y72Caracteres() {
		when(encoder.encode(anyString())).thenReturn("hash");
		when(usuarios.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.registrar("ana", "a@b.co", "12345678", "1");
		service.registrar("ana2", "a2@b.co", "x".repeat(72), "1");

		verify(usuarios, times(2)).save(any());
	}

	@Test
	void rechazaContrasenasFueraDeRango() {
		assertThrows(IllegalArgumentException.class, () -> service.registrar("ana", "a@b.co", "1234567", "1"));
		assertThrows(IllegalArgumentException.class, () -> service.registrar("ana", "a@b.co", "x".repeat(73), "1"));
		assertThrows(IllegalArgumentException.class, () -> service.registrar("ana", "a@b.co", null, "1"));

		verify(usuarios, never()).save(any());
	}

	@Test
	void rechazaCorreosYUsuariosConFormatoInvalido() {
		assertThrows(IllegalArgumentException.class, () -> service.registrar("ana", "sin-arroba", "Clave1234", "1"));
		assertThrows(IllegalArgumentException.class, () -> service.registrar("ana", "a@b", "Clave1234", "1"));
		assertThrows(IllegalArgumentException.class, () -> service.registrar("ana", null, "Clave1234", "1"));
		assertThrows(IllegalArgumentException.class, () -> service.registrar("a!", "a@b.co", "Clave1234", "1"));
		assertThrows(IllegalArgumentException.class, () -> service.registrar("ab", "a@b.co", "Clave1234", "1"));

		verify(usuarios, never()).save(any());
	}

	@Test
	void rechazaCorreoDuplicado() {
		when(usuarios.findByCorreo("a@b.co")).thenReturn(Optional.of(new Usuario()));

		assertThrows(IllegalArgumentException.class, () -> service.registrar("ana", "a@b.co", "Clave1234", "1"));

		verify(usuarios, never()).save(any());
	}

	@Test
	void rechazaNombreDeUsuarioDuplicado() {
		when(usuarios.findByCorreo("a@b.co")).thenReturn(Optional.empty());
		when(usuarios.findByNombreUsuario("ana")).thenReturn(Optional.of(new Usuario()));

		assertThrows(IllegalArgumentException.class, () -> service.registrar("ana", "a@b.co", "Clave1234", "1"));
	}

	@Test
	void iniciaSesionConLaContrasenaCorrecta() {
		when(usuarios.findByCorreo("a@b.co")).thenReturn(Optional.of(ana()));
		when(encoder.matches("Clave1234", "hash")).thenReturn(true);

		var resultado = service.iniciarSesion("A@B.co", "Clave1234");

		assertEquals("u1", resultado.id());
		assertEquals("ana", resultado.nombreUsuario());
	}

	@Test
	void rechazaCredencialesInvalidas() {
		when(usuarios.findByCorreo("a@b.co")).thenReturn(Optional.of(ana()));
		when(encoder.matches("incorrecta", "hash")).thenReturn(false);

		assertThrows(IllegalArgumentException.class, () -> service.iniciarSesion("a@b.co", "incorrecta"));
	}

	@Test
	void rechazaUsuarioInexistenteConElMismoMensaje() {
		when(usuarios.findByCorreo("x@b.co")).thenReturn(Optional.empty());

		var ex = assertThrows(IllegalArgumentException.class, () -> service.iniciarSesion("x@b.co", "Clave1234"));

		assertEquals("Credenciales inválidas", ex.getMessage());
	}

	@Test
	void bloqueaElLoginTrasVariosIntentosFallidos() {
		when(usuarios.findByCorreo("a@b.co")).thenReturn(Optional.of(ana()));
		when(encoder.matches(anyString(), anyString())).thenReturn(false);

		for (int i = 0; i < CuentaService.MAX_INTENTOS; i++) {
			assertThrows(IllegalArgumentException.class, () -> service.iniciarSesion("a@b.co", "mala"));
		}
		// ya bloqueado: ni siquiera se comprueba la contraseña
		assertThrows(IllegalStateException.class, () -> service.iniciarSesion("a@b.co", "Clave1234"));

		verify(encoder, times(CuentaService.MAX_INTENTOS)).matches(anyString(), anyString());
	}

	@Test
	void unLoginCorrectoReiniciaElContadorDeIntentos() {
		when(usuarios.findByCorreo("a@b.co")).thenReturn(Optional.of(ana()));
		when(encoder.matches(eq("mala"), anyString())).thenReturn(false);
		when(encoder.matches(eq("buena"), anyString())).thenReturn(true);

		for (int i = 0; i < CuentaService.MAX_INTENTOS - 1; i++) {
			assertThrows(IllegalArgumentException.class, () -> service.iniciarSesion("a@b.co", "mala"));
		}
		service.iniciarSesion("a@b.co", "buena");
		for (int i = 0; i < CuentaService.MAX_INTENTOS - 1; i++) {
			assertThrows(IllegalArgumentException.class, () -> service.iniciarSesion("a@b.co", "mala"));
		}

		// después de 4 + éxito + 4 fallos todavía no está bloqueado
		assertEquals("u1", service.iniciarSesion("a@b.co", "buena").id());
	}

	@Test
	void fusionaCarritoVisitanteAlIniciarSesion() {
		Carrito visitante = new Carrito("visitante", null);

		service.fusionarCarrito("u1", visitante);

		verify(carritos).fusionar("u1", visitante);
	}
}
