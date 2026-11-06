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

import com.metcoffee.dto.DireccionDTO;
import com.metcoffee.model.Direccion;
import com.metcoffee.repository.DireccionRepository;

@ExtendWith(MockitoExtension.class)
class DomicilioServiceTest {

	@Mock
	DireccionRepository direcciones;

	@InjectMocks
	DomicilioService service;

	@Test
	void guardaDireccion() {
		when(direcciones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		assertEquals("Calle 1", service.guardar("u",
				new DireccionDTO(null, "Casa", "Ana", "Calle 1", "1", true)).direccion());

		verify(direcciones).save(any());
	}

	@Test
	void guardaDireccionPrincipalYDesmarcaLaAnterior() {
		Direccion anterior = new Direccion("d1", "u", "Casa", "Ana", "Calle 1", "1", true);
		when(direcciones.findByUsuarioId("u")).thenReturn(List.of(anterior));
		when(direcciones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.guardar("u", new DireccionDTO(null, "Trabajo", "Ana", "Calle 2", "2", true));

		assertEquals(false, anterior.isPrincipal());
		verify(direcciones).save(anterior);
	}

	@Test
	void actualizaDireccionPropia() {
		Direccion direccion = new Direccion("d", "u", "Casa", "Ana", "Calle 1", "1", true);
		when(direcciones.findByIdAndUsuarioId("d", "u")).thenReturn(Optional.of(direccion));
		when(direcciones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var resultado = service.actualizar("u", "d",
				new DireccionDTO("d", "Trabajo", "Ana", "Calle 2", "2", false));

		assertEquals("Calle 2", resultado.direccion());
		assertEquals("Trabajo", direccion.getEtiqueta());
		verify(direcciones).save(direccion);
	}

	@Test
	void rechazaActualizarDireccionAjena() {
		when(direcciones.findByIdAndUsuarioId("d", "u")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.actualizar("u", "d",
				new DireccionDTO("d", "Trabajo", "Ana", "Calle 2", "2", false)));

		verify(direcciones, never()).save(any());
	}

	@Test
	void seleccionaDireccionPrincipalYDesmarcaLasDemas() {
		Direccion casa = new Direccion("c", "u", "Casa", "Ana", "Calle 1", "1", true);
		Direccion trabajo = new Direccion("t", "u", "Trabajo", "Ana", "Calle 2", "2", false);
		when(direcciones.findByIdAndUsuarioId("t", "u")).thenReturn(Optional.of(trabajo));
		when(direcciones.findByUsuarioId("u")).thenReturn(List.of(casa, trabajo));

		service.seleccionarPrincipal("u", "t");

		assertEquals(false, casa.isPrincipal());
		assertEquals(true, trabajo.isPrincipal());
		verify(direcciones).save(casa);
		verify(direcciones).save(trabajo);
	}
}
