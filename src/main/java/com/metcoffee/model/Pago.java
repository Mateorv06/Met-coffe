package com.metcoffee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("pagos")
public class Pago {

	@Id
	private String id;
	private String pedidoId;
	private String referencia;
	private double monto;
	private EstadoPago estado;

	public Pago() {
	}

	public Pago(String pedidoId, double monto, EstadoPago estado, String referencia) {
		this.pedidoId = pedidoId;
		this.monto = monto;
		this.estado = estado;
		this.referencia = referencia;
	}

	public String getId() { return id; }
	public String getPedidoId() { return pedidoId; }
	public String getReferencia() { return referencia; }
	public double getMonto() { return monto; }
	public EstadoPago getEstado() { return estado; }

	public void setEstado(EstadoPago estado) { this.estado = estado; }
}
