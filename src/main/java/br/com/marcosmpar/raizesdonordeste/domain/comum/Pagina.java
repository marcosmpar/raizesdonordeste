package br.com.marcosmpar.raizesdonordeste.domain.comum;

import java.util.List;
import java.util.function.Function;

/** Página de resultados independente de framework. page é 1-based. */
public record Pagina<T>(List<T> conteudo, int page, int limit, long totalElementos) {

	public int totalPaginas() {
		return (int) Math.ceil((double) totalElementos / limit);
	}

	public <R> Pagina<R> map(Function<T, R> fn) {
		return new Pagina<>(conteudo.stream().map(fn).toList(), page, limit, totalElementos);
	}
}
