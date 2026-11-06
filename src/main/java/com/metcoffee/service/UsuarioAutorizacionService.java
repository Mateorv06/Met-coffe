package com.metcoffee.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class UsuarioAutorizacionService {

	public void verificarPropietario(String usuarioId, Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()
				|| !usuarioId.equals(authentication.getName())) {
			throw new AccessDeniedException("Acceso denegado");
		}
	}
}