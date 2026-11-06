package com.metcoffee.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.metcoffee.dto.UsuarioDTO;
import com.metcoffee.model.Carrito;
import com.metcoffee.model.Rol;
import com.metcoffee.model.Usuario;
import com.metcoffee.repository.UsuarioRepository;

@Service
public class CuentaService {

	public static final int PASSWORD_MIN = 8;
	public static final int PASSWORD_MAX = 72; // límite de bcrypt
	public static final int MAX_INTENTOS = 5;
	public static final Duration BLOQUEO = Duration.ofMinutes(15);

	private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final Pattern USUARIO = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");

	private final UsuarioRepository usuarios;
	private final CarritoService carritos;
	private final PasswordEncoder encoder;
	private final Map<String, Intentos> intentos = new ConcurrentHashMap<>();

	public CuentaService(UsuarioRepository usuarios, CarritoService carritos, PasswordEncoder encoder) {
		this.usuarios = usuarios;
		this.carritos = carritos;
		this.encoder = encoder;
	}

	public UsuarioDTO registrar(String nombreUsuario, String correo, String password, String telefono) {
		validarRegistro(nombreUsuario, correo, password);
		String correoNormalizado = correo.trim().toLowerCase();
		if (usuarios.findByCorreo(correoNormalizado).isPresent()
				|| usuarios.findByNombreUsuario(nombreUsuario.trim()).isPresent()) {
			throw new IllegalArgumentException("La cuenta ya existe");
		}
		Usuario nuevo = new Usuario(null, nombreUsuario.trim(), correoNormalizado, encoder.encode(password), telefono,
				Rol.CLIENTE);
		return dto(usuarios.save(nuevo));
	}

	public UsuarioDTO iniciarSesion(String correo, String password) {
		if (correo == null || password == null) {
			throw new IllegalArgumentException("Credenciales inválidas");
		}
		String clave = correo.trim().toLowerCase();
		Instant ahora = Instant.now();

		Intentos actual = intentos.get(clave);
		if (actual != null && actual.bloqueadoHasta != null && actual.bloqueadoHasta.isAfter(ahora)) {
			throw new IllegalStateException("Demasiados intentos fallidos; intenta de nuevo más tarde");
		}

		Usuario u = usuarios.findByCorreo(clave)
				.filter(x -> encoder.matches(password, x.getPassword()))
				.orElse(null);
		if (u == null) {
			registrarFallo(clave, ahora);
			throw new IllegalArgumentException("Credenciales inválidas");
		}
		intentos.remove(clave);
		return dto(u);
	}

	public void fusionarCarrito(String usuario, Carrito visitante) {
		carritos.fusionar(usuario, visitante);
	}

	// ----------------------------------------------------------------- auxiliares

	private void validarRegistro(String nombreUsuario, String correo, String password) {
		if (correo == null || !CORREO.matcher(correo.trim()).matches()) {
			throw new IllegalArgumentException("Correo inválido");
		}
		if (nombreUsuario == null || !USUARIO.matcher(nombreUsuario.trim()).matches()) {
			throw new IllegalArgumentException("Nombre de usuario inválido (3 a 30 caracteres: letras, números, . _ -)");
		}
		if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
			throw new IllegalArgumentException(
					"La contraseña debe tener entre " + PASSWORD_MIN + " y " + PASSWORD_MAX + " caracteres");
		}
	}

	private void registrarFallo(String clave, Instant ahora) {
		intentos.compute(clave, (k, previo) -> {
			Intentos i = previo == null ? new Intentos() : previo;
			// un bloqueo ya vencido reinicia la cuenta
			if (i.bloqueadoHasta != null && !i.bloqueadoHasta.isAfter(ahora)) {
				i = new Intentos();
			}
			i.fallos++;
			if (i.fallos >= MAX_INTENTOS) {
				i.bloqueadoHasta = ahora.plus(BLOQUEO);
			}
			return i;
		});
	}

	private UsuarioDTO dto(Usuario u) {
		return new UsuarioDTO(u.getId(), u.getNombreUsuario(), u.getCorreo(), u.getTelefono(), u.getRol());
	}

	private static final class Intentos {
		int fallos;
		Instant bloqueadoHasta;
	}
}
