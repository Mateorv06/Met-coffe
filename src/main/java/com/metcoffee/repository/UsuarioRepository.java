package com.metcoffee.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.metcoffee.model.Rol;
import com.metcoffee.model.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

	Optional<Usuario> findByCorreo(String correo);

	Optional<Usuario> findByNombreUsuario(String nombreUsuario);

	long countByRol(Rol rol);
}
