package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasarelaPagoSimuladaTest {

	private final PasarelaPagoSimulada pasarela = new PasarelaPagoSimulada();

	@Test
	void apruebaMontosPositivos() {
		assertTrue(pasarela.procesar("SIM-1", 25000));
	}

	@Test
	void rechazaMontosInvalidosOSinReferencia() {
		assertFalse(pasarela.procesar("SIM-1", 0));
		assertFalse(pasarela.procesar("SIM-1", -5));
		assertFalse(pasarela.procesar("SIM-1", Double.NaN));
		assertFalse(pasarela.procesar(null, 100));
	}
}
