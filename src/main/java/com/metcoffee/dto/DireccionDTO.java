package com.metcoffee.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record DireccionDTO(String id,@NotBlank @Size(max = 40) String etiqueta,
		@NotBlank @Size(max = 120) String destinatario,
		@NotBlank @Size(max = 200) String direccion,
		@NotBlank @Size(max = 30) String telefono,boolean principal) { }
