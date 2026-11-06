package com.metcoffee.dto;
import java.util.List; import com.metcoffee.model.Producto;
public record DashboardDTO(double ingresos,long pedidos,long clientes,List<Producto> bajoStock) { }
