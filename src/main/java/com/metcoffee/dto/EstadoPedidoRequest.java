package com.metcoffee.dto;

import com.metcoffee.model.EstadoPedido;

import jakarta.validation.constraints.NotNull;

public record EstadoPedidoRequest(@NotNull(message = "El estado es obligatorio") EstadoPedido estado) {
}
