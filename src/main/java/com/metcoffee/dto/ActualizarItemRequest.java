package com.metcoffee.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ActualizarItemRequest(
		@Min(value = 1, message = "La cantidad mínima es 1") @Max(value = 99, message = "La cantidad máxima es 99") int cantidad,
		@NotBlank(message = "La molienda es obligatoria") String molienda,
		double gramos) {
}
