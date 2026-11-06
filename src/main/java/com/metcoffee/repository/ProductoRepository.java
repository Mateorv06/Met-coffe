package com.metcoffee.repository;
import java.util.*; import org.springframework.data.mongodb.repository.MongoRepository; import com.metcoffee.model.Producto;
public interface ProductoRepository extends MongoRepository<Producto,String>{ List<Producto> findByActivoTrue(); List<Producto> findByActivoTrueAndNovedadTrue(); }
