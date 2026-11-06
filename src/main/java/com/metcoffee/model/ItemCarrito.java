package com.metcoffee.model;
public record ItemCarrito(String productoId, int cantidad, String molienda, double gramosPorUnidad, double precioUnitario, BolsaPersonalizada bolsa) { public boolean esBolsa(){return bolsa!=null;} }
