package com.metcoffee.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.metcoffee.model.EstadoPedido;
import com.metcoffee.model.Pedido;

public interface PedidoRepository extends MongoRepository<Pedido, String> {

	List<Pedido> findByUsuarioIdOrderByFechaDesc(String usuarioId);

	List<Pedido> findByEstado(EstadoPedido estado);

	long countByEstado(EstadoPedido estado);

	boolean existsByUsuarioId(String usuarioId);
}
