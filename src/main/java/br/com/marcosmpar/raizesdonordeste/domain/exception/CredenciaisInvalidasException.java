package br.com.marcosmpar.raizesdonordeste.domain.exception;

/** Login com e-mail/senha incorretos ou usuário inativo (mapeado para 401). */
public class CredenciaisInvalidasException extends DomainException {

	public CredenciaisInvalidasException() {
		super("CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.");
	}
}
