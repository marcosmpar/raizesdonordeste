package br.com.marcosmpar.raizesdonordeste.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import br.com.marcosmpar.raizesdonordeste.infrastructure.security.JwtAuthenticationFilter;
import br.com.marcosmpar.raizesdonordeste.infrastructure.security.JwtService;
import tools.jackson.databind.json.JsonMapper;

import br.com.marcosmpar.raizesdonordeste.api.error.ErroFactory;
import br.com.marcosmpar.raizesdonordeste.api.error.ErroResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Base de segurança: API stateless, 401/403 no padrão de erro JSON.
 * Autenticação via JWT (filtro) e perfis por rota via @PreAuthorize.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String[] PUBLICO = { "/auth/**", "/v3/api-docs/**", "/swagger-ui.html",
			"/swagger-ui/**" };

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, AuthenticationEntryPoint entryPoint,
			AccessDeniedHandler deniedHandler, JwtService jwtService) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a.requestMatchers(PUBLICO).permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
						.requestMatchers(HttpMethod.GET, "/unidades/**", "/produtos/**", "/promocoes").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationEntryPoint authenticationEntryPoint(ErroFactory erros, JsonMapper mapper) {
		return (request, response, ex) -> escrever(response, HttpStatus.UNAUTHORIZED, mapper,
				erros.criar(request, "NAO_AUTENTICADO", "Autenticação necessária ou inválida."));
	}

	@Bean
	AccessDeniedHandler accessDeniedHandler(ErroFactory erros, JsonMapper mapper) {
		return (request, response, ex) -> escrever(response, HttpStatus.FORBIDDEN, mapper,
				erros.criar(request, "ACESSO_NEGADO", "Sem permissão para este recurso."));
	}

	private static void escrever(HttpServletResponse response, HttpStatus status, JsonMapper mapper,
			ErroResponse corpo) throws java.io.IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		mapper.writeValue(response.getWriter(), corpo);
	}
}
