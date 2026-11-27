package br.com.marcosmpar.raizesdonordeste.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Lê "Authorization: Bearer <jwt>"; se válido, autentica. Se inválido, segue sem autenticar (→ 401). */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String PREFIXO = "Bearer ";

	private final JwtService jwt;

	public JwtAuthenticationFilter(JwtService jwt) {
		this.jwt = jwt;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(PREFIXO)) {
			jwt.validar(header.substring(PREFIXO.length()).trim()).ifPresent(u -> {
				var auth = new UsernamePasswordAuthenticationToken(u, null,
						List.of(new SimpleGrantedAuthority(u.perfil().authority())));
				SecurityContextHolder.getContext().setAuthentication(auth);
			});
		}
		chain.doFilter(request, response);
	}
}
