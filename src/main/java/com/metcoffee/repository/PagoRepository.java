package com.metcoffee.repository;
import java.util.*; import org.springframework.data.mongodb.repository.MongoRepository; import com.metcoffee.model.Pago;
public interface PagoRepository extends MongoRepository<Pago,String>{ Optional<Pago> findByPedidoId(String pedidoId); }
