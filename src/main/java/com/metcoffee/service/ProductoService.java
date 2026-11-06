package com.metcoffee.service;

import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.metcoffee.dto.ProductoDTO;
import com.metcoffee.model.Producto;
import com.metcoffee.repository.ProductoRepository;

@Service
public class ProductoService {

	public static final long IMAGEN_MAX_BYTES = 5L * 1024 * 1024;
	private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp");

	private final ProductoRepository productos;

	@Value("${app.upload-dir:uploads}")
	private String uploadDir;

	public ProductoService(ProductoRepository productos) {
		this.productos = productos;
	}

	public List<ProductoDTO> listar() {
		return productos.findAll().stream().map(this::dto).toList();
	}

	public ProductoDTO guardar(ProductoDTO d) {
		if (d == null || d.nombre() == null || d.nombre().isBlank() || !Double.isFinite(d.precioBase())
				|| d.precioBase() <= 0 || !Double.isFinite(d.stockGramos()) || d.stockGramos() < 0) {
			throw new IllegalArgumentException("Producto inválido");
		}
		Producto p = new Producto(d.id(), d.nombre(), d.precioBase(), d.stockGramos());
		p.setOrigen(d.origen());
		p.setPerfil(d.perfil());
		p.setImagen(d.imagen());
		p.setNovedad(d.novedad());
		p.setActivo(d.activo());
		return dto(productos.save(p));
	}

	public void desactivar(String id) {
		Producto p = buscar(id);
		p.setActivo(false);
		productos.save(p);
	}

	/**
	 * Asocia una imagen al producto. Valida extensión (jpg, jpeg, png, webp) y tamaño (máx. 5 MB).
	 * El archivo se guarda en la carpeta configurable de uploads y la ruta relativa queda en MongoDB.
	 */
	public ProductoDTO subirImagen(String id, String nombreArchivo, long tamanoBytes) {
		return subirImagen(id, nombreArchivo, tamanoBytes, null);
	}

	public ProductoDTO subirImagen(String id, String nombreArchivo, long tamanoBytes, byte[] contenido) {
		if (nombreArchivo == null || !nombreArchivo.contains(".")) {
			throw new IllegalArgumentException("Archivo de imagen inválido");
		}
		String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase();
		if (!EXTENSIONES.contains(extension)) {
			throw new IllegalArgumentException("Formato de imagen no permitido (jpg, jpeg, png o webp)");
		}
		if (tamanoBytes <= 0 || tamanoBytes > IMAGEN_MAX_BYTES) {
			throw new IllegalArgumentException("La imagen debe pesar entre 1 byte y 5 MB");
		}
		Producto p = buscar(id);
		String nombreSeguro = id + "." + extension;
		if (contenido != null) {
			try {
				Path carpeta = Paths.get(uploadDir).resolve("productos").normalize();
				Files.createDirectories(carpeta);
				Files.write(carpeta.resolve(nombreSeguro), contenido);
			} catch (IOException ex) {
				throw new IllegalStateException("No se pudo guardar la imagen", ex);
			}
		}
		p.setImagen("/uploads/productos/" + nombreSeguro);
		return dto(productos.save(p));
	}

	private Producto buscar(String id) {
		return productos.findById(id).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
	}

	private ProductoDTO dto(Producto p) {
		return new ProductoDTO(p.getId(), p.getNombre(), p.getOrigen(), p.getPerfil(), p.getImagen(),
				p.getPrecioBase(), p.getStockGramos(), p.isNovedad(), p.isActivo());
	}
}
