package com.metcoffee.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public record AjusteStockRequest(@PositiveOrZero double stockGramos, String nota) {
	public AjusteStockRequest {
		if (nota != null) {
			nota = nota.trim();
		}
	}
}
