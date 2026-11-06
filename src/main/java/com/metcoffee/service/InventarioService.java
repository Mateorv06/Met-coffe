package com.metcoffee.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.metcoffee.model.MovimientoInventario;
import com.metcoffee.model.Producto;
import com.metcoffee.model.TipoMovimiento;
import com.metcoffee.repository.MovimientoInventarioRepository;
import com.metcoffee.repository.ProductoRepository;

@Service
public class InventarioService {

	private final ProductoRepository productos;
	private final MovimientoInventarioRepository movimientos;

	public InventarioService(ProductoRepository productos, MovimientoInventarioRepository movimientos) {
		this.productos = productos;
		this.movimientos = movimientos;
	}

	public Producto consultar(String id) {
		return productos.findById(id).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
	}

	public Producto ajustar(String id, double nuevo, String nota) {
		if (!Double.isFinite(nuevo) || nuevo < 0) {
			throw new IllegalArgumentException("Stock inválido");
		}
		Producto p = consultar(id);
		double delta = nuevo - p.getStockGramos();
		p.setStockGramos(nuevo);
		productos.save(p);
		movimientos.save(new MovimientoInventario(null, id, null, TipoMovimiento.AJUSTE_MANUAL, delta, Instant.now(), nota));
		return p;
	}

	public void descontar(Producto p, double gramos, String pedido) {
		validarGramos(p, gramos);
		if (p.getStockGramos() < gramos) {
			throw new IllegalStateException("Stock insuficiente");
		}
		p.setStockGramos(p.getStockGramos() - gramos);
		productos.save(p);
		movimientos.save(new MovimientoInventario(null, p.getId(), pedido, TipoMovimiento.VENTA, -gramos, Instant.now(),
				"Compra confirmada"));
	}

	public void devolver(Producto p, double gramos, String pedido) {
		validarGramos(p, gramos);
		p.setStockGramos(p.getStockGramos() + gramos);
		productos.save(p);
		movimientos.save(new MovimientoInventario(null, p.getId(), pedido, TipoMovimiento.DEVOLUCION_CANCELACION, gramos,
				Instant.now(), "Cancelación"));
	}

	/** Historial de movimientos de todo el inventario, del más reciente al más antiguo. */
	public List<MovimientoInventario> listarMovimientos() {
		return movimientos.findAllByOrderByFechaDesc();
	}

	/** Historial de movimientos de un producto, del más reciente al más antiguo. */
	public List<MovimientoInventario> listarMovimientos(String productoId) {
		consultar(productoId);
		return movimientos.findByProductoIdOrderByFechaDesc(productoId);
	}

	private void validarGramos(Producto p, double gramos) {
		if (p == null) {
			throw new IllegalArgumentException("Producto no encontrado");
		}
		if (!Double.isFinite(gramos) || gramos <= 0) {
			throw new IllegalArgumentException("Cantidad inválida");
		}
	}
}
