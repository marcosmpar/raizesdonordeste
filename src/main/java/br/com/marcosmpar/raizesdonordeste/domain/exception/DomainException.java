package br.com.marcosmpar.raizesdonordeste.domain.exception;

/** Base das exceções de regra de negócio. Não depende de HTTP: a camada API as traduz. */
public abstract class DomainException extends RuntimeException {

	private final String code;

	protected DomainException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
