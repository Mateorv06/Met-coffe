package com.metcoffee.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.metcoffee.dto.PedidoDTO;
import com.metcoffee.model.CompartimentoBolsa;
import com.metcoffee.model.DetallePedido;
import com.metcoffee.model.EstadoPago;
import com.metcoffee.model.EstadoPedido;
import com.metcoffee.model.Pedido;
import com.metcoffee.repository.PagoRepository;
import com.metcoffee.repository.PedidoRepository;
import com.metcoffee.repository.ProductoRepository;

@Service
public class PedidoService {

	private final PedidoRepository pedidos;
	private final PagoRepository pagos;
	private final ProductoRepository productos;
	private final InventarioService inventario;

	public PedidoService(PedidoRepository pedidos, PagoRepository pagos, ProductoRepository productos,
			InventarioService inventario) {
		this.pedidos = pedidos;
		this.pagos = pagos;
		this.productos = productos;
		this.inventario = inventario;
	}

	public List<PedidoDTO> historial(String usuario) {
		return pedidos.findByUsuarioIdOrderByFechaDesc(usuario).stream().map(PedidoService::dto).toList();
	}

	public PedidoDTO detalle(String usuario, String id) {
		return dto(propio(usuario, id));
	}

	/** Cancelación del cliente: solo si el pedido está en Procesando. Restaura stock y reembolsa el pago. */
	public void cancelar(String usuario, String id) {
		Pedido p = propio(usuario, id);
		if (p.getEstado() != EstadoPedido.PROCESANDO) {
			throw new IllegalStateException("El pedido no se puede cancelar");
		}
		p.setEstado(EstadoPedido.CANCELADO);
		pedidos.save(p);

		pagos.findByPedidoId(id).ifPresent(pago -> {
			if (pago.getEstado() == EstadoPago.APROBADO) {
				pago.setEstado(EstadoPago.REEMBOLSADO);
				pagos.save(pago);
			}
		});

		for (DetallePedido d : p.getItems()) {
			if (d.bolsa() != null) {
				for (CompartimentoBolsa c : d.bolsa().compartimentos()) {
					productos.findById(c.productoId()).ifPresent(x -> inventario.devolver(x, c.gramos(), id));
				}
			} else if (d.productoId() != null) {
				productos.findById(d.productoId())
						.ifPresent(x -> inventario.devolver(x, d.gramosPorUnidad() * d.cantidad(), id));
			}
		}
	}

	private Pedido propio(String usuario, String id) {
		return pedidos.findById(id).filter(x -> x.getUsuarioId().equals(usuario))
				.orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
	}

	static PedidoDTO dto(Pedido p) {
		return new PedidoDTO(p.getId(), p.getTotal(), p.getEstado(), p.getFecha(), p.getItems(),
				p.getDireccionEnvio(), p.getDestinatario(), p.getTelefonoEnvio());
	}
}
