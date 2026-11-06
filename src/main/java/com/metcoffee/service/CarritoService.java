package com.metcoffee.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.metcoffee.dto.CarritoDTO;
import com.metcoffee.model.BolsaPersonalizada;
import com.metcoffee.model.Carrito;
import com.metcoffee.model.ItemCarrito;
import com.metcoffee.model.Presentacion;
import com.metcoffee.model.Producto;
import com.metcoffee.repository.CarritoRepository;
import com.metcoffee.repository.ProductoRepository;

@Service
public class CarritoService {

	/** Cantidad máxima permitida por ítem del carrito. */
	public static final int CANTIDAD_MAXIMA = 99;

	private final CarritoRepository carritos;
	private final ProductoRepository productos;

	public CarritoService(CarritoRepository carritos, ProductoRepository productos) {
		this.carritos = carritos;
		this.productos = productos;
	}

	public CarritoDTO consultar(String usuario) {
		return dto(obtener(usuario));
	}

	public Carrito obtener(String usuario) {
		return carritos.findByUsuarioId(usuario).orElseGet(() -> new Carrito(null, usuario));
	}

	/** Agrega un producto en una presentación fija (125, 250 o 500 g). */
	public CarritoDTO agregarProducto(String usuario, String productoId, int cantidad, String molienda, double gramos) {
		validarCantidad(cantidad);
		validarMolienda(molienda);
		Presentacion presentacion = Presentacion.deGramos(gramos);
		Producto p = productoActivo(productoId);

		Carrito c = obtener(usuario);
		List<ItemCarrito> items = new ArrayList<>(c.getItems());

		int existente = indiceLinea(items, productoId, molienda, gramos);
		int nuevaCantidad = cantidad + (existente >= 0 ? items.get(existente).cantidad() : 0);
		validarCantidad(nuevaCantidad);
		validarStock(p, items, existente, nuevaCantidad * gramos);

		ItemCarrito nuevo = new ItemCarrito(productoId, nuevaCantidad, molienda, gramos,
				presentacion.precio(p.getPrecioBase()), null);
		if (existente >= 0) {
			items.set(existente, nuevo);
		} else {
			items.add(nuevo);
		}
		c.setItems(items);
		return dto(carritos.save(c));
	}

	public CarritoDTO agregarBolsa(String usuario, BolsaPersonalizada bolsa) {
		if (bolsa == null || bolsa.compartimentos() == null || bolsa.compartimentos().isEmpty()) {
			throw new IllegalArgumentException("Bolsa inválida");
		}
		Carrito c = obtener(usuario);
		List<ItemCarrito> items = new ArrayList<>(c.getItems());
		items.add(new ItemCarrito(null, 1, null, bolsa.gramos(), bolsa.precio(), bolsa));
		c.setItems(items);
		return dto(carritos.save(c));
	}

	public CarritoDTO actualizarProducto(String usuario, int indice, int cantidad, String molienda, double gramos) {
		validarCantidad(cantidad);
		validarMolienda(molienda);
		Presentacion presentacion = Presentacion.deGramos(gramos);

		Carrito c = obtener(usuario);
		List<ItemCarrito> items = new ArrayList<>(c.getItems());
		validarIndice(indice, items);

		ItemCarrito actual = items.get(indice);
		if (actual.esBolsa()) {
			throw new IllegalArgumentException("No se puede editar una bolsa como producto");
		}
		Producto producto = productoActivo(actual.productoId());
		validarStock(producto, items, indice, cantidad * gramos);

		items.set(indice, new ItemCarrito(actual.productoId(), cantidad, molienda, gramos,
				presentacion.precio(producto.getPrecioBase()), null));
		c.setItems(items);
		return dto(carritos.save(c));
	}

	public void eliminar(String usuario, int indice) {
		Carrito c = obtener(usuario);
		List<ItemCarrito> items = new ArrayList<>(c.getItems());
		validarIndice(indice, items);
		items.remove(indice);
		c.setItems(items);
		carritos.save(c);
	}

	public void limpiar(String usuario) {
		Carrito c = obtener(usuario);
		c.setItems(List.of());
		carritos.save(c);
	}

	/** Une el carrito del visitante con el del usuario; las líneas iguales suman cantidad (máx. 99). */
	public void fusionar(String usuario, Carrito visitante) {
		if (visitante == null) {
			throw new IllegalArgumentException("Carrito visitante inválido");
		}
		Carrito c = obtener(usuario);
		List<ItemCarrito> items = new ArrayList<>(c.getItems());

		for (ItemCarrito nuevo : visitante.getItems()) {
			int existente = nuevo.esBolsa() ? -1
					: indiceLinea(items, nuevo.productoId(), nuevo.molienda(), nuevo.gramosPorUnidad());
			if (existente >= 0) {
				ItemCarrito actual = items.get(existente);
				int suma = Math.min(CANTIDAD_MAXIMA, actual.cantidad() + nuevo.cantidad());
				items.set(existente, new ItemCarrito(actual.productoId(), suma, actual.molienda(),
						actual.gramosPorUnidad(), actual.precioUnitario(), null));
			} else {
				items.add(nuevo);
			}
		}
		c.setItems(items);
		carritos.save(c);
	}

	// ----------------------------------------------------------------- auxiliares

	private Producto productoActivo(String productoId) {
		return productos.findById(productoId).filter(Producto::isActivo)
				.orElseThrow(() -> new IllegalArgumentException("Producto no disponible"));
	}

	private void validarCantidad(int cantidad) {
		if (cantidad <= 0 || cantidad > CANTIDAD_MAXIMA) {
			throw new IllegalArgumentException("Cantidad inválida: debe estar entre 1 y " + CANTIDAD_MAXIMA);
		}
	}

	private void validarMolienda(String molienda) {
		if (!"Grano".equals(molienda) && !"Molido".equals(molienda)) {
			throw new IllegalArgumentException("Molienda inválida");
		}
	}

	private void validarIndice(int indice, List<ItemCarrito> items) {
		if (indice < 0 || indice >= items.size()) {
			throw new IllegalArgumentException("Ítem inexistente");
		}
	}

	private int indiceLinea(List<ItemCarrito> items, String productoId, String molienda, double gramos) {
		for (int i = 0; i < items.size(); i++) {
			ItemCarrito x = items.get(i);
			if (!x.esBolsa() && productoId != null && productoId.equals(x.productoId())
					&& molienda != null && molienda.equals(x.molienda())
					&& Double.compare(x.gramosPorUnidad(), gramos) == 0) {
				return i;
			}
		}
		return -1;
	}

	/** El stock debe alcanzar para esta línea más lo que el mismo producto ya ocupa en otras líneas. */
	private void validarStock(Producto p, List<ItemCarrito> items, int indiceExcluido, double gramosLinea) {
		double enOtrasLineas = 0;
		for (int i = 0; i < items.size(); i++) {
			ItemCarrito x = items.get(i);
			if (i != indiceExcluido && !x.esBolsa() && p.getId() != null && p.getId().equals(x.productoId())) {
				enOtrasLineas += x.cantidad() * x.gramosPorUnidad();
			}
		}
		if (p.getStockGramos() < gramosLinea + enOtrasLineas) {
			throw new IllegalStateException("Stock insuficiente");
		}
	}

	private CarritoDTO dto(Carrito c) {
		double total = c.getItems().stream().mapToDouble(x -> x.cantidad() * x.precioUnitario()).sum();
		return new CarritoDTO(c.getId(), c.getUsuarioId(), c.getItems(), total);
	}
}
