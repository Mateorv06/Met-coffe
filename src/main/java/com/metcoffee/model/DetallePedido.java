package com.metcoffee.model;
public record DetallePedido(String productoId,String nombreProducto,int cantidad,double gramosPorUnidad,double precioUnitario,BolsaPersonalizada bolsa){ public double subtotal(){return cantidad*precioUnitario;} }
