package com.metcoffee.service;

/** Contrato de la pasarela de pago. En el proyecto solo existe una simulación. */
public interface PasarelaPago {

	/** @return true si el pago fue aprobado, false si fue rechazado. */
	boolean procesar(String referencia, double monto);
}
