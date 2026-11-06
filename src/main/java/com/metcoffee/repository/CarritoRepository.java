package com.metcoffee.repository;
import java.util.*; import org.springframework.data.mongodb.repository.MongoRepository; import com.metcoffee.model.Carrito;
public interface CarritoRepository extends MongoRepository<Carrito,String>{ Optional<Carrito> findByUsuarioId(String usuarioId); }
