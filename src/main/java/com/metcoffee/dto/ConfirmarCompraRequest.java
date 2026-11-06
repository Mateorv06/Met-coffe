package com.metcoffee.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmarCompraRequest(@NotBlank(message = "El domicilio es obligatorio") String direccionId) {
}
