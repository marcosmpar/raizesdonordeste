package br.com.marcosmpar.raizesdonordeste.domain.exception;

/** Recurso referenciado não existe (mapeado para 404). */
public class RecursoNaoEncontradoException extends DomainException {

	public RecursoNaoEncontradoException(String recurso, Object id) {
		super("RECURSO_NAO_ENCONTRADO", recurso + " não encontrado(a): " + id);
	}
}
