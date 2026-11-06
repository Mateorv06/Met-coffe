package com.metcoffee.dto;
import java.util.List; import com.metcoffee.model.ItemCarrito;
public record CarritoDTO(String id,String usuarioId,List<ItemCarrito> items,double total) { }
