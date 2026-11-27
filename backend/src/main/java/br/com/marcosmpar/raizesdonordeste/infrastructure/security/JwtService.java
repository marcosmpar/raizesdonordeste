package br.com.marcosmpar.raizesdonordeste.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.marcosmpar.raizesdonordeste.application.auth.EmissorToken;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService implements EmissorToken {

	private final SecretKey chave;
	private final long expiracaoSegundos;

	public JwtService(@Value("${app.security.jwt.secret}") String secret,
			@Value("${app.security.jwt.expiration-seconds}") long expiracaoSegundos) {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("JWT_SECRET deve ter no mínimo 32 caracteres.");
		}
		this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expiracaoSegundos = expiracaoSegundos;
	}

	@Override
	public String emitir(Usuario usuario) {
		Instant agora = Instant.now();
		return Jwts.builder()
				.subject(String.valueOf(usuario.getId()))
				.claim("email", usuario.getEmail())
				.claim("perfil", usuario.getPerfil().name())
				.issuedAt(Date.from(agora))
				.expiration(Date.from(agora.plusSeconds(expiracaoSegundos)))
				.signWith(chave)
				.compact();
	}

	@Override
	public long expiracaoEmSegundos() {
		return expiracaoSegundos;
	}

	/** Valida assinatura e expiração; vazio se o token for inválido. */
	public Optional<UsuarioAutenticado> validar(String token) {
		try {
			Claims c = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
			return Optional.of(new UsuarioAutenticado(Long.valueOf(c.getSubject()), c.get("email", String.class),
					Perfil.valueOf(c.get("perfil", String.class))));
		}
		catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}
}
