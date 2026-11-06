package com.metcoffee.model;
import java.time.Instant; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document;
@Document("movimientos_inventario") public record MovimientoInventario(@Id String id,String productoId,String pedidoId,TipoMovimiento tipo,double gramos,Instant fecha,String nota) { }
