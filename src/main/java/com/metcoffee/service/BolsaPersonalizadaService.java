package com.metcoffee.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.metcoffee.dto.BolsaDTO;
import com.metcoffee.model.BolsaPersonalizada;
import com.metcoffee.model.CompartimentoBolsa;
import com.metcoffee.model.Presentacion;
import com.metcoffee.model.Producto;
import com.metcoffee.repository.ProductoRepository;

@Service
public class BolsaPersonalizadaService {

	private final ProductoRepository productos;

	public BolsaPersonalizadaService(ProductoRepository productos) {
		this.productos = productos;
	}

	public BolsaPersonalizada configurar(BolsaDTO dto) {
		if (dto == null || dto.tamano() == null || dto.tamano().isBlank()
				|| !(dto.compartimentos() == 2 || dto.compartimentos() == 4)
				|| dto.componentes() == null || dto.componentes().size() != dto.compartimentos()) {
			throw new IllegalArgumentException("Configuración de bolsa inválida");
		}

		Presentacion presentacion = Presentacion.deNombre(dto.tamano());
		double gramos = presentacion.getGramos();
		double porcion = gramos / dto.compartimentos();

		double sumaPreciosBase = 0;
		Map<String, Double> gramosPorProducto = new HashMap<>();
		List<CompartimentoBolsa> compartimentos = new ArrayList<>();

		for (BolsaDTO.CompartimentoDTO c : dto.componentes()) {
			Producto p = productos.findById(c.productoId())
					.filter(Producto::isActivo)
					.orElseThrow(() -> new IllegalArgumentException("Producto no disponible"));
			if (!"Grano".equals(c.molienda()) && !"Molido".equals(c.molienda())) {
				throw new IllegalArgumentException("Molienda inválida");
			}
			double acumulado = gramosPorProducto.merge(p.getId() == null ? c.productoId() : p.getId(), porcion, Double::sum);
			if (p.getStockGramos() < acumulado) {
				throw new IllegalStateException("Stock insuficiente");
			}
			sumaPreciosBase += p.getPrecioBase();
			compartimentos.add(new CompartimentoBolsa(c.productoId(), c.molienda(), porcion));
		}

		double precio = presentacion.precio(sumaPreciosBase / dto.compartimentos());
		return new BolsaPersonalizada(dto.tamano(), gramos, compartimentos, precio);
	}
}
