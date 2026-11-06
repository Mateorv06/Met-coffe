package com.metcoffee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metcoffee.dto.ProductoDTO;
import com.metcoffee.model.Producto;
import com.metcoffee.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

	@Mock
	ProductoRepository productos;

	@InjectMocks
	ProductoService service;

	@Test
	void guardaProducto() {
		ProductoDTO dto = new ProductoDTO(null, "Café", "Huila", "Perfil", "x", 10, 1, false, true);
		when(productos.save(any())).thenAnswer(invocation -> {
			var producto = invocation.getArgument(0, Producto.class);
			producto.setId("p");
			return producto;
		});

		assertEquals("p", service.guardar(dto).id());

		verify(productos).save(any());
	}

	@Test
	void rechazaDatosInvalidos() {
		assertThrows(IllegalArgumentException.class,
				() -> service.guardar(new ProductoDTO(null, "Café", "Huila", "Perfil", "x", 0, 1, false, true)));
		assertThrows(IllegalArgumentException.class,
				() -> service.guardar(new ProductoDTO(null, "Café", "Huila", "Perfil", "x", 10, -1, false, true)));
		assertThrows(IllegalArgumentException.class,
				() -> service.guardar(new ProductoDTO(null, " ", "Huila", "Perfil", "x", 10, 1, false, true)));
		assertThrows(IllegalArgumentException.class, () -> service.guardar(null));

		verify(productos, never()).save(any());
	}

	@Test
	void listaTodosLosProductos() {
		Producto inactivo = new Producto("q", "Otro", 10, 5);
		inactivo.setActivo(false);
		when(productos.findAll()).thenReturn(List.of(new Producto("p", "Café", 10, 5), inactivo));

		assertEquals(2, service.listar().size());
	}

	@Test
	void desactivaProductoSinBorrarlo() {
		Producto producto = new Producto("p", "Café", 10, 5);
		when(productos.findById("p")).thenReturn(Optional.of(producto));

		service.desactivar("p");

		assertFalse(producto.isActivo());
		verify(productos).save(producto);
	}

	@Test
	void rechazaDesactivarUnProductoInexistente() {
		when(productos.findById("x")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.desactivar("x"));
	}

	@Test
	void subeLaImagenDeUnProducto() {
		Producto producto = new Producto("p", "Café", 10, 5);
		when(productos.findById("p")).thenReturn(Optional.of(producto));
		when(productos.save(producto)).thenReturn(producto);

		var resultado = service.subirImagen("p", "FOTO.PNG", 1024);

		assertEquals("/uploads/productos/p.png", resultado.imagen());
		verify(productos).save(producto);
	}

	@Test
	void rechazaImagenesInvalidas() {
		assertThrows(IllegalArgumentException.class, () -> service.subirImagen("p", "foto.gif", 1024));
		assertThrows(IllegalArgumentException.class, () -> service.subirImagen("p", "virus.exe", 1024));
		assertThrows(IllegalArgumentException.class, () -> service.subirImagen("p", "sinextension", 1024));
		assertThrows(IllegalArgumentException.class, () -> service.subirImagen("p", null, 1024));
		assertThrows(IllegalArgumentException.class, () -> service.subirImagen("p", "foto.jpg", 0));
		assertThrows(IllegalArgumentException.class,
				() -> service.subirImagen("p", "foto.jpg", ProductoService.IMAGEN_MAX_BYTES + 1));

		verify(productos, never()).save(any());
	}

	@Test
	void rechazaSubirImagenAUnProductoInexistente() {
		when(productos.findById("x")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.subirImagen("x", "foto.jpg", 1024));
	}
}
