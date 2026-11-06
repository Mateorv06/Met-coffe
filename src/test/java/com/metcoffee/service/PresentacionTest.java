package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.metcoffee.model.Presentacion;

class PresentacionTest {

	@ParameterizedTest
	@CsvSource({ "125,PEQUENA,5.0", "250,MEDIANA,10.0", "500,GRANDE,18.0" })
	void aplicaElFactorDePrecioDeCadaTamano(double gramos, Presentacion esperada, double precioConBase10) {
		Presentacion presentacion = Presentacion.deGramos(gramos);

		assertEquals(esperada, presentacion);
		assertEquals(precioConBase10, presentacion.precio(10));
	}

	@Test
	void rechazaGramajesQueNoSonPresentaciones() {
		assertThrows(IllegalArgumentException.class, () -> Presentacion.deGramos(300));
		assertThrows(IllegalArgumentException.class, () -> Presentacion.deGramos(0));
	}

	@Test
	void reconoceLosNombresDeTamano() {
		assertEquals(Presentacion.PEQUENA, Presentacion.deNombre("pequeña"));
		assertEquals(Presentacion.PEQUENA, Presentacion.deNombre("small"));
		assertEquals(Presentacion.MEDIANA, Presentacion.deNombre("Mediana"));
		assertEquals(Presentacion.GRANDE, Presentacion.deNombre("500"));
		assertThrows(IllegalArgumentException.class, () -> Presentacion.deNombre("xl"));
		assertThrows(IllegalArgumentException.class, () -> Presentacion.deNombre(" "));
		assertThrows(IllegalArgumentException.class, () -> Presentacion.deNombre(null));
	}

	@Test
	void redondeaElPrecioADosDecimales() {
		assertEquals(59999.4, Presentacion.GRANDE.precio(33333));
	}
}
