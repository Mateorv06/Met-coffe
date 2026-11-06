package com.metcoffee.repository;
import java.util.*; import org.springframework.data.mongodb.repository.MongoRepository; import com.metcoffee.model.Direccion;
public interface DireccionRepository extends MongoRepository<Direccion,String>{ List<Direccion> findByUsuarioId(String usuarioId); Optional<Direccion> findByIdAndUsuarioId(String id,String usuarioId); }
