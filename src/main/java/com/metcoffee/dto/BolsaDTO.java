package com.metcoffee.dto;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
public record BolsaDTO(@NotBlank String tamano,@Min(1) @Max(4) int compartimentos,
		@NotEmpty @Valid List<CompartimentoDTO> componentes) {
	public record CompartimentoDTO(@NotBlank String productoId,@NotBlank String molienda){}
}
