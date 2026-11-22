package br.com.marcosmpar.raizesdonordeste.application.pagamento;

public class GatewayIndisponivelException extends RuntimeException {

	public GatewayIndisponivelException(String message) {
		super(message);
	}
}
