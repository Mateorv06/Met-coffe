package com.metcoffee.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistroRequest(
		@NotBlank(message = "El nombre de usuario es obligatorio") String nombreUsuario,
		@NotBlank(message = "El correo es obligatorio") String correo,
		@NotBlank(message = "La contraseña es obligatoria") String password,
		String telefono) {
}
