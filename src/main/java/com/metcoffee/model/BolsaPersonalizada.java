package com.metcoffee.model;
import java.util.List;
public record BolsaPersonalizada(String tamano, double gramos, List<CompartimentoBolsa> compartimentos, double precio) { }
