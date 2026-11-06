package com.metcoffee.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
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
public class PedidoAdministrativoService {

	/** Tiempo que debe pasar en estado Enviado antes de pasar solo a Entregado. */
	public static final Duration ESPERA_ENTREGA = Duration.ofMinutes(5);

	private final PedidoRepository pedidos;
	private final PagoRepository pagos;
	private final ProductoRepository productos;
	private final InventarioService inventario;

	public PedidoAdministrativoService(PedidoRepository pedidos, PagoRepository pagos, ProductoRepository productos,
			InventarioService inventario) {
		this.pedidos = pedidos;
		this.pagos = pagos;
		this.productos = productos;
		this.inventario = inventario;
	}

	public List<PedidoDTO> listar() {
		return pedidos.findAll().stream().map(PedidoService::dto).toList();
	}

	public PedidoDTO detalle(String id) {
		return PedidoService.dto(buscar(id));
	}

	/**
	 * Transiciones permitidas: Procesando → Enviado | Cancelado, y Enviado → Entregado.
	 * Cancelar restaura el stock y reembolsa el pago.
	 */
	public PedidoDTO actualizarEstado(String id, EstadoPedido nuevo) {
		if (nuevo == null) {
			throw new IllegalArgumentException("Estado inválido");
		}
		Pedido p = buscar(id);
		if (!transicionPermitida(p.getEstado(), nuevo)) {
			throw new IllegalStateException("Transición no permitida: " + p.getEstado() + " → " + nuevo);
		}

		if (nuevo == EstadoPedido.CANCELADO) {
			reembolsarPago(id);
			restaurarStock(p);
		}
		if (nuevo == EstadoPedido.ENVIADO) {
			p.setFechaEnvio(Instant.now());
		}
		p.setEstado(nuevo);
		pedidos.save(p);
		return PedidoService.dto(p);
	}

	/** Pasa a Entregado los pedidos que llevan Enviados al menos ESPERA_ENTREGA. */
	public int avanzarEnviadosAEntregado(Instant ahora) {
		int avanzados = 0;
		for (Pedido p : pedidos.findByEstado(EstadoPedido.ENVIADO)) {
			Instant desde = p.getFechaEnvio() != null ? p.getFechaEnvio() : p.getFecha();
			if (!desde.plus(ESPERA_ENTREGA).isAfter(ahora)) {
				p.setEstado(EstadoPedido.ENTREGADO);
				pedidos.save(p);
				avanzados++;
			}
		}
		return avanzados;
	}

	@Scheduled(fixedDelay = 60_000)
	public void entregarAutomaticamente() {
		avanzarEnviadosAEntregado(Instant.now());
	}

	// ----------------------------------------------------------------- auxiliares

	private Pedido buscar(String id) {
		return pedidos.findById(id).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
	}

	private boolean transicionPermitida(EstadoPedido actual, EstadoPedido nuevo) {
		return switch (actual) {
			case PROCESANDO -> nuevo == EstadoPedido.ENVIADO || nuevo == EstadoPedido.CANCELADO;
			case ENVIADO -> nuevo == EstadoPedido.ENTREGADO;
			case ENTREGADO, CANCELADO -> false;
		};
	}

	private void reembolsarPago(String pedidoId) {
		pagos.findByPedidoId(pedidoId).ifPresent(pago -> {
			if (pago.getEstado() == EstadoPago.APROBADO) {
				pago.setEstado(EstadoPago.REEMBOLSADO);
				pagos.save(pago);
			}
		});
	}

	private void restaurarStock(Pedido pedido) {
		for (DetallePedido d : pedido.getItems()) {
			if (d.bolsa() != null) {
				for (CompartimentoBolsa c : d.bolsa().compartimentos()) {
					productos.findById(c.productoId()).ifPresent(x -> inventario.devolver(x, c.gramos(), pedido.getId()));
				}
			} else if (d.productoId() != null) {
				productos.findById(d.productoId())
						.ifPresent(x -> inventario.devolver(x, d.gramosPorUnidad() * d.cantidad(), pedido.getId()));
			}
		}
	}
}
