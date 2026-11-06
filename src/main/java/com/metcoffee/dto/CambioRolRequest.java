package com.metcoffee.dto;

import com.metcoffee.model.Rol;

import jakarta.validation.constraints.NotNull;

public record CambioRolRequest(@NotNull(message = "El rol es obligatorio") Rol rol) {
}
