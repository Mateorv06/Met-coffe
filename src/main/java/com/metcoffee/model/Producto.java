package com.metcoffee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("productos")
public class Producto {
    @Id private String id;
    private String nombre, origen, perfil, imagen;
    private double precioBase, stockGramos;
    private boolean novedad, activo = true;
    public Producto() { }
    public Producto(String id, String nombre, double precioBase, double stockGramos) { this.id=id; this.nombre=nombre; this.precioBase=precioBase; this.stockGramos=stockGramos; }
    public String getId(){return id;} public String getNombre(){return nombre;} public String getOrigen(){return origen;} public String getPerfil(){return perfil;} public String getImagen(){return imagen;} public double getPrecioBase(){return precioBase;} public double getStockGramos(){return stockGramos;} public boolean isNovedad(){return novedad;} public boolean isActivo(){return activo;}
    public void setId(String v){id=v;} public void setNombre(String v){nombre=v;} public void setOrigen(String v){origen=v;} public void setPerfil(String v){perfil=v;} public void setImagen(String v){imagen=v;} public void setPrecioBase(double v){precioBase=v;} public void setStockGramos(double v){stockGramos=v;} public void setNovedad(boolean v){novedad=v;} public void setActivo(boolean v){activo=v;}
}
