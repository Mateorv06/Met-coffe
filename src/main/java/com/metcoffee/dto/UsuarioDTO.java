package com.metcoffee.dto;
import com.metcoffee.model.Rol;
public record UsuarioDTO(String id,String nombreUsuario,String correo,String telefono,Rol rol) { }
