package br.com.marcosmpar.raizesdonordeste.api.error;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ErroFactory {

	public ErroResponse criar(HttpServletRequest request, String error, String message,
			List<ErroResponse.Detalhe> details) {
		Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE);
		return new ErroResponse(error, message, details, Instant.now(), request.getRequestURI(),
				requestId != null ? requestId.toString() : null);
	}

	public ErroResponse criar(HttpServletRequest request, String error, String message) {
		return criar(request, error, message, List.of());
	}
}
