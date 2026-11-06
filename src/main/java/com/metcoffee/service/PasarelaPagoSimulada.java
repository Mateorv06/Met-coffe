package com.metcoffee.service;

import org.springframework.stereotype.Component;

/** Pago simulado: aprueba cualquier monto válido y rechaza montos no positivos. */
@Component
public class PasarelaPagoSimulada implements PasarelaPago {

	@Override
	public boolean procesar(String referencia, double monto) {
		return referencia != null && Double.isFinite(monto) && monto > 0;
	}
}
