package com.metcoffee.dto;

import java.time.Instant;
import java.util.List;

import com.metcoffee.model.DetallePedido;
import com.metcoffee.model.EstadoPedido;

public record PedidoDTO(String id, double total, EstadoPedido estado, Instant fecha, List<DetallePedido> items,
		String direccionEnvio, String destinatario, String telefonoEnvio) {
}
