package br.com.marcosmpar.raizesdonordeste.api.comum;

import java.util.List;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

public record PaginaResponse<T>(List<T> content, int page, int limit, long totalElements, int totalPages) {

	public static <D, T> PaginaResponse<T> de(Pagina<D> pagina, java.util.function.Function<D, T> fn) {
		var p = pagina.map(fn);
		return new PaginaResponse<>(p.conteudo(), p.page(), p.limit(), p.totalElementos(), p.totalPaginas());
	}
}
