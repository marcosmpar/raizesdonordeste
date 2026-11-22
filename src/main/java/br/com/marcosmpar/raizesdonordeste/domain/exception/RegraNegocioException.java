package br.com.marcosmpar.raizesdonordeste.domain.exception;

import java.util.List;

/** Violação de regra de negócio/conflito de estado (mapeado para 409). Ex.: ESTOQUE_INSUFICIENTE. */
public class RegraNegocioException extends DomainException {

	private final transient List<Detalhe> detalhes;

	public RegraNegocioException(String code, String message) {
		this(code, message, List.of());
	}

	public RegraNegocioException(String code, String message, List<Detalhe> detalhes) {
		super(code, message);
		this.detalhes = detalhes;
	}

	public List<Detalhe> getDetalhes() {
		return detalhes;
	}

	public record Detalhe(String field, String issue) {
	}
}
