package com.metcoffee.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
public record ProductoDTO(String id,@NotBlank @Size(max = 120) String nombre,String origen,String perfil,String imagen,
		@Positive double precioBase,@PositiveOrZero double stockGramos,boolean novedad,boolean activo) { }
