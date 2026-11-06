package com.metcoffee.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.metcoffee.dto.DashboardDTO;
import com.metcoffee.model.EstadoPedido;
import com.metcoffee.model.Pedido;
import com.metcoffee.model.Rol;
import com.metcoffee.repository.PedidoRepository;
import com.metcoffee.repository.ProductoRepository;
import com.metcoffee.repository.UsuarioRepository;

@Service
public class DashboardService {

	private final PedidoRepository pedidos;
	private final UsuarioRepository usuarios;
	private final ProductoRepository productos;

	public DashboardService(PedidoRepository pedidos, UsuarioRepository usuarios, ProductoRepository productos) {
		this.pedidos = pedidos;
		this.usuarios = usuarios;
		this.productos = productos;
	}

	/** Ingresos = solo pedidos Entregados; clientes = solo usuarios con rol Cliente. */
	public DashboardDTO consultar(double limiteBajoStock) {
		List<Pedido> todos = pedidos.findAll();
		double ingresos = todos.stream()
				.filter(p -> p.getEstado() == EstadoPedido.ENTREGADO)
				.mapToDouble(Pedido::getTotal)
				.sum();
		return new DashboardDTO(ingresos, todos.size(), usuarios.countByRol(Rol.CLIENTE),
				productos.findByActivoTrue().stream().filter(p -> p.getStockGramos() <= limiteBajoStock).toList());
	}
}
