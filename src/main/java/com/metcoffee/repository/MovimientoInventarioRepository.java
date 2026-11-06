package com.metcoffee.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.metcoffee.model.MovimientoInventario;

public interface MovimientoInventarioRepository extends MongoRepository<MovimientoInventario, String> {

	List<MovimientoInventario> findAllByOrderByFechaDesc();

	List<MovimientoInventario> findByProductoIdOrderByFechaDesc(String productoId);
}
