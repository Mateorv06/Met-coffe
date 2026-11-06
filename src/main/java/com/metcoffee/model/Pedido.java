package com.metcoffee.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("pedidos")
public class Pedido {

	@Id
	private String id;
	private String usuarioId;
	private String direccionEnvio;
	private String destinatario;
	private String telefonoEnvio;
	private List<DetallePedido> items = new ArrayList<>();
	private double total;
	private EstadoPedido estado = EstadoPedido.PROCESANDO;
	private Instant fecha = Instant.now();
	private Instant fechaEnvio;

	public Pedido() {
	}

	public Pedido(String id, String usuarioId, String direccionEnvio, List<DetallePedido> items, double total) {
		this(id, usuarioId, direccionEnvio, null, null, items, total);
	}

	public Pedido(String id, String usuarioId, String direccionEnvio, String destinatario, String telefonoEnvio,
			List<DetallePedido> items, double total) {
		this.id = id;
		this.usuarioId = usuarioId;
		this.direccionEnvio = direccionEnvio;
		this.destinatario = destinatario;
		this.telefonoEnvio = telefonoEnvio;
		this.items = items;
		this.total = total;
	}

	public String getId() { return id; }
	public String getUsuarioId() { return usuarioId; }
	public String getDireccionEnvio() { return direccionEnvio; }
	public String getDestinatario() { return destinatario; }
	public String getTelefonoEnvio() { return telefonoEnvio; }
	public List<DetallePedido> getItems() { return items; }
	public double getTotal() { return total; }
	public EstadoPedido getEstado() { return estado; }
	public Instant getFecha() { return fecha; }
	public Instant getFechaEnvio() { return fechaEnvio; }

	public void setEstado(EstadoPedido estado) { this.estado = estado; }
	public void setFecha(Instant fecha) { this.fecha = fecha; }
	public void setFechaEnvio(Instant fechaEnvio) { this.fechaEnvio = fechaEnvio; }
}
