package com.metcoffee.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.metcoffee.dto.PedidoDTO;
import com.metcoffee.model.Carrito;
import com.metcoffee.model.CompartimentoBolsa;
import com.metcoffee.model.DetallePedido;
import com.metcoffee.model.Direccion;
import com.metcoffee.model.EstadoPago;
import com.metcoffee.model.ItemCarrito;
import com.metcoffee.model.Pago;
import com.metcoffee.model.Pedido;
import com.metcoffee.model.Presentacion;
import com.metcoffee.model.Producto;
import com.metcoffee.repository.CarritoRepository;
import com.metcoffee.repository.DireccionRepository;
import com.metcoffee.repository.PagoRepository;
import com.metcoffee.repository.PedidoRepository;
import com.metcoffee.repository.ProductoRepository;

@Service
public class CompraService {

	private final CarritoRepository carritos;
	private final DireccionRepository direcciones;
	private final ProductoRepository productos;
	private final PedidoRepository pedidos;
	private final PagoRepository pagos;
	private final InventarioService inventario;
	private final PasarelaPago pasarela;

	public CompraService(CarritoRepository carritos, DireccionRepository direcciones, ProductoRepository productos,
			PedidoRepository pedidos, PagoRepository pagos, InventarioService inventario, PasarelaPago pasarela) {
		this.carritos = carritos;
		this.direcciones = direcciones;
		this.productos = productos;
		this.pedidos = pedidos;
		this.pagos = pagos;
		this.inventario = inventario;
		this.pasarela = pasarela;
	}

	@Transactional
	public PedidoDTO confirmar(String usuario, String direccionId) {
		Carrito carrito = carritos.findByUsuarioId(usuario)
				.filter(x -> !x.getItems().isEmpty())
				.orElseThrow(() -> new IllegalStateException("Carrito vacío"));
		Direccion direccion = direcciones.findByIdAndUsuarioId(direccionId, usuario)
				.orElseThrow(() -> new IllegalArgumentException("Domicilio no encontrado"));

		// 1. Validar precios y disponibilidad (acumulando los gramos que pide cada producto)
		Map<String, Producto> catalogo = new HashMap<>();
		Map<String, Double> requerido = new HashMap<>();
		List<DetallePedido> detalles = new ArrayList<>();
		for (ItemCarrito item : carrito.getItems()) {
			detalles.add(item.esBolsa() ? validarBolsa(item, catalogo, requerido)
					: validarProducto(item, catalogo, requerido));
		}
		requerido.forEach((id, gramos) -> {
			if (catalogo.get(id).getStockGramos() < gramos) {
				throw new IllegalStateException("Stock insuficiente");
			}
		});
		double total = detalles.stream().mapToDouble(DetallePedido::subtotal).sum();

		// 2. Pago simulado: si falla no se confirma la compra ni se toca el inventario
		String referencia = "SIM-" + UUID.randomUUID();
		if (!pasarela.procesar(referencia, total)) {
			pagos.save(new Pago(null, total, EstadoPago.FALLIDO, referencia));
			throw new IllegalStateException("El pago simulado fue rechazado; la compra no se confirmó");
		}

		// 3. Registrar pedido, descontar stock, registrar pago y vaciar carrito
		Pedido pedido = pedidos.save(new Pedido(null, usuario, direccion.getDireccion(), direccion.getDestinatario(),
				direccion.getTelefono(), detalles, total));

		for (ItemCarrito item : carrito.getItems()) {
			if (item.esBolsa()) {
				for (CompartimentoBolsa c : item.bolsa().compartimentos()) {
					inventario.descontar(catalogo.get(c.productoId()), c.gramos(), pedido.getId());
				}
			}
		}
		for (Map.Entry<String, Double> e : gramosPorProductoSueltos(carrito).entrySet()) {
			inventario.descontar(catalogo.get(e.getKey()), e.getValue(), pedido.getId());
		}

		pagos.save(new Pago(pedido.getId(), total, EstadoPago.APROBADO, referencia));
		carrito.setItems(List.of());
		carritos.save(carrito);

		return new PedidoDTO(pedido.getId(), total, pedido.getEstado(), pedido.getFecha(), detalles,
				pedido.getDireccionEnvio(), pedido.getDestinatario(), pedido.getTelefonoEnvio());
	}

	// ----------------------------------------------------------------- auxiliares

	private DetallePedido validarProducto(ItemCarrito i, Map<String, Producto> catalogo, Map<String, Double> requerido) {
		Producto p = cargar(i.productoId(), catalogo);
		double esperado = Presentacion.deGramos(i.gramosPorUnidad()).precio(p.getPrecioBase());
		if (Math.abs(i.precioUnitario() - esperado) > 0.01) {
			throw new IllegalStateException("Precio inválido");
		}
		requerido.merge(p.getId(), i.gramosPorUnidad() * i.cantidad(), Double::sum);
		return new DetallePedido(p.getId(), p.getNombre(), i.cantidad(), i.gramosPorUnidad(), i.precioUnitario(), null);
	}

	private DetallePedido validarBolsa(ItemCarrito i, Map<String, Producto> catalogo, Map<String, Double> requerido) {
		double precioBasePonderado = 0;
		for (CompartimentoBolsa c : i.bolsa().compartimentos()) {
			Producto p = cargar(c.productoId(), catalogo);
			precioBasePonderado += p.getPrecioBase() * (c.gramos() / i.bolsa().gramos());
			requerido.merge(p.getId(), c.gramos(), Double::sum);
		}
		double esperado = Presentacion.deGramos(i.bolsa().gramos()).precio(precioBasePonderado);
		if (Math.abs(i.precioUnitario() - esperado) > 0.01) {
			throw new IllegalStateException("Precio inválido");
		}
		return new DetallePedido(null, "Bolsa personalizada", 1, i.gramosPorUnidad(), i.precioUnitario(), i.bolsa());
	}

	private Producto cargar(String id, Map<String, Producto> catalogo) {
		Producto cargado = catalogo.get(id);
		if (cargado != null) {
			return cargado;
		}
		Producto p = productos.findById(id).filter(Producto::isActivo)
				.orElseThrow(() -> new IllegalArgumentException("Producto no disponible"));
		catalogo.put(id, p);
		return p;
	}

	private Map<String, Double> gramosPorProductoSueltos(Carrito carrito) {
		Map<String, Double> gramos = new HashMap<>();
		for (ItemCarrito i : carrito.getItems()) {
			if (!i.esBolsa()) {
				gramos.merge(i.productoId(), i.gramosPorUnidad() * i.cantidad(), Double::sum);
			}
		}
		return gramos;
	}
}
