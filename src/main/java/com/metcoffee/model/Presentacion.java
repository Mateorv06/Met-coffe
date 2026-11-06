package com.metcoffee.model;

/**
 * Presentaciones de venta del café. Cada una tiene un gramaje fijo y un factor
 * que se multiplica por el precio base del producto (precio base = 250 g).
 */
public enum Presentacion {

	PEQUENA(125, 0.5),
	MEDIANA(250, 1.0),
	GRANDE(500, 1.8);

	private final double gramos;
	private final double factorPrecio;

	Presentacion(double gramos, double factorPrecio) {
		this.gramos = gramos;
		this.factorPrecio = factorPrecio;
	}

	public double getGramos() {
		return gramos;
	}

	public double getFactorPrecio() {
		return factorPrecio;
	}

	/** Precio unitario de esta presentación, redondeado a 2 decimales. */
	public double precio(double precioBase) {
		return Math.round(precioBase * factorPrecio * 100.0) / 100.0;
	}

	public static Presentacion deGramos(double gramos) {
		for (Presentacion p : values()) {
			if (Double.compare(p.gramos, gramos) == 0) {
				return p;
			}
		}
		throw new IllegalArgumentException("Tamaño inválido: solo se permiten 125, 250 o 500 g");
	}

	public static Presentacion deNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("Tamaño inválido");
		}
		return switch (nombre.trim().toLowerCase()) {
			case "small", "pequena", "pequeña", "125" -> PEQUENA;
			case "medium", "mediana", "250" -> MEDIANA;
			case "large", "grande", "500" -> GRANDE;
			default -> throw new IllegalArgumentException("Tamaño inválido");
		};
	}
}
