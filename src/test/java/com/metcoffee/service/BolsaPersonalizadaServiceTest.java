package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metcoffee.dto.BolsaDTO;
import com.metcoffee.model.Producto;
import com.metcoffee.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class BolsaPersonalizadaServiceTest {

	@Mock
	ProductoRepository productos;

	@InjectMocks
	BolsaPersonalizadaService service;

	private BolsaDTO.CompartimentoDTO comp(String productoId, String molienda) {
		return new BolsaDTO.CompartimentoDTO(productoId, molienda);
	}

	@Test
	void configuraBolsa() {
		Producto producto = new Producto("p", "Café", 40000, 1000);
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		var bolsa = service.configurar(new BolsaDTO("mediana", 2, List.of(comp("p", "Grano"), comp("p", "Molido"))));

		assertEquals(250, bolsa.gramos());
		assertEquals(2, bolsa.compartimentos().size());
		assertEquals(125, bolsa.compartimentos().get(0).gramos());
		assertEquals(40000, bolsa.precio());
		verify(productos, times(2)).findById("p");
	}

	@Test
	void aplicaElFactorDePrecioSegunElTamanoYPromediaLosCafes() {
		Producto a = new Producto("a", "Café A", 40000, 1000);
		Producto b = new Producto("b", "Café B", 20000, 1000);
		when(productos.findById("a")).thenReturn(Optional.of(a));
		when(productos.findById("b")).thenReturn(Optional.of(b));

		var grande = service.configurar(new BolsaDTO("grande", 4,
				List.of(comp("a", "Grano"), comp("a", "Grano"), comp("b", "Molido"), comp("b", "Molido"))));
		var pequena = service.configurar(new BolsaDTO("pequeña", 2, List.of(comp("a", "Grano"), comp("b", "Grano"))));

		assertEquals(500, grande.gramos());
		assertEquals(125, grande.compartimentos().get(0).gramos());
		assertEquals(54000, grande.precio()); // promedio 30000 x 1.8
		assertEquals(125, pequena.gramos());
		assertEquals(15000, pequena.precio()); // promedio 30000 x 0.5
	}

	@Test
	void rechazaTamanoNulo() {
		assertThrows(IllegalArgumentException.class,
				() -> service.configurar(new BolsaDTO(null, 2, List.of(comp("p", "Grano"), comp("p", "Molido")))));
	}

	@Test
	void rechazaTamanoDesconocido() {
		assertThrows(IllegalArgumentException.class,
				() -> service.configurar(new BolsaDTO("xl", 2, List.of(comp("p", "Grano"), comp("p", "Molido")))));
	}

	@Test
	void soloAceptaDosOCuatroCompartimentos() {
		assertThrows(IllegalArgumentException.class, () -> service.configurar(
				new BolsaDTO("mediana", 3, List.of(comp("p", "Grano"), comp("p", "Grano"), comp("p", "Grano")))));
		assertThrows(IllegalArgumentException.class, () -> service.configurar(null));
	}

	@Test
	void exigeUnComponentePorCompartimento() {
		assertThrows(IllegalArgumentException.class,
				() -> service.configurar(new BolsaDTO("mediana", 4, List.of(comp("p", "Grano"), comp("p", "Grano")))));
	}

	@Test
	void rechazaMoliendaInvalida() {
		when(productos.findById("p")).thenReturn(Optional.of(new Producto("p", "Café", 10, 1000)));

		assertThrows(IllegalArgumentException.class,
				() -> service.configurar(new BolsaDTO("mediana", 2, List.of(comp("p", "Fina"), comp("p", "Grano")))));
	}

	@Test
	void rechazaProductoInactivo() {
		Producto inactivo = new Producto("p", "Café", 10, 1000);
		inactivo.setActivo(false);
		when(productos.findById("p")).thenReturn(Optional.of(inactivo));

		var ex = assertThrows(IllegalArgumentException.class,
				() -> service.configurar(new BolsaDTO("mediana", 2, List.of(comp("p", "Grano"), comp("p", "Molido")))));

		assertEquals("Producto no disponible", ex.getMessage());
	}

	@Test
	void rechazaProductoInexistente() {
		when(productos.findById("x")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class,
				() -> service.configurar(new BolsaDTO("mediana", 2, List.of(comp("x", "Grano"), comp("x", "Molido")))));
	}

	@Test
	void rechazaStockInsuficienteAcumulandoLosCompartimentosDelMismoCafe() {
		when(productos.findById("p")).thenReturn(Optional.of(new Producto("p", "Café", 10, 200)));

		assertThrows(IllegalStateException.class,
				() -> service.configurar(new BolsaDTO("mediana", 2, List.of(comp("p", "Grano"), comp("p", "Molido")))));
	}
}
