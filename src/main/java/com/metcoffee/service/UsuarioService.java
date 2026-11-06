package com.metcoffee.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.metcoffee.dto.UsuarioDTO;
import com.metcoffee.model.Rol;
import com.metcoffee.model.Usuario;
import com.metcoffee.repository.CarritoRepository;
import com.metcoffee.repository.DireccionRepository;
import com.metcoffee.repository.PedidoRepository;
import com.metcoffee.repository.UsuarioRepository;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarios;
	private final PedidoRepository pedidos;
	private final CarritoRepository carritos;
	private final DireccionRepository direcciones;

	public UsuarioService(UsuarioRepository usuarios, PedidoRepository pedidos, CarritoRepository carritos,
			DireccionRepository direcciones) {
		this.usuarios = usuarios;
		this.pedidos = pedidos;
		this.carritos = carritos;
		this.direcciones = direcciones;
	}

	public List<UsuarioDTO> listar() {
		return usuarios.findAll().stream().map(this::dto).toList();
	}

	public UsuarioDTO consultar(String id) {
		return dto(buscar(id));
	}

	/** Un administrador no puede cambiar su propio rol. */
	public UsuarioDTO cambiarRol(String actorId, String id, Rol nuevoRol) {
		if (nuevoRol == null) {
			throw new IllegalArgumentException("Rol inválido");
		}
		if (id != null && id.equals(actorId)) {
			throw new IllegalStateException("No puedes cambiar tu propio rol");
		}
		Usuario u = buscar(id);
		u.setRol(nuevoRol);
		return dto(usuarios.save(u));
	}

	/** No se puede eliminar el propio usuario ni un usuario que tenga pedidos. */
	public void eliminar(String actorId, String id) {
		if (id != null && id.equals(actorId)) {
			throw new IllegalStateException("No puedes eliminar tu propio usuario");
		}
		Usuario u = buscar(id);
		if (pedidos.existsByUsuarioId(id)) {
			throw new IllegalStateException("No se puede eliminar un usuario con pedidos");
		}
		carritos.findByUsuarioId(id).ifPresent(carritos::delete);
		direcciones.deleteAll(direcciones.findByUsuarioId(id));
		usuarios.delete(u);
	}

	private Usuario buscar(String id) {
		return usuarios.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
	}

	private UsuarioDTO dto(Usuario u) {
		return new UsuarioDTO(u.getId(), u.getNombreUsuario(), u.getCorreo(), u.getTelefono(), u.getRol());
	}
}
