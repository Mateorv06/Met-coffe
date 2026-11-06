package com.metcoffee.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.metcoffee.dto.UsuarioDTO;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final Duration expiration;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration:PT30M}") Duration expiration) {
		this.signingKey = buildKey(secret);
		this.expiration = expiration;
	}

	public String generateToken(UsuarioDTO usuario) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(usuario.id())
				.claim("role", usuario.rol().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(expiration)))
				.signWith(signingKey)
				.compact();
	}

	public Claims parse(String token) {
		return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
	}

	private SecretKey buildKey(String secret) {
		try {
			byte[] decoded = Decoders.BASE64.decode(secret);
			return Keys.hmacShaKeyFor(decoded);
		} catch (DecodingException | IllegalArgumentException ex) {
			byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
			return Keys.hmacShaKeyFor(raw);
		}
	}
}
