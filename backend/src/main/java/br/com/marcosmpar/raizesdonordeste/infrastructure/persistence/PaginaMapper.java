package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

final class PaginaMapper {

	private PaginaMapper() {
	}

	/** page do domínio é 1-based; Spring Data é 0-based. */
	static Pageable pageable(int page, int limit, Sort sort) {
		return PageRequest.of(page - 1, limit, sort);
	}

	static Pageable pageable(int page, int limit) {
		return PageRequest.of(page - 1, limit);
	}

	static <T> Pagina<T> pagina(Page<T> p) {
		return new Pagina<>(p.getContent(), p.getNumber() + 1, p.getSize(), p.getTotalElements());
	}
}
