package br.com.marcosmpar.raizesdonordeste.application.pagamento;

import java.math.BigDecimal;

import br.com.marcosmpar.raizesdonordeste.domain.pagamento.SimulacaoPagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.FormaPagamento;

/** Porta para o serviço externo de pagamento (implementada por um MOCK na infraestrutura). */
public interface GatewayPagamento {

	record Solicitacao(Long pedidoId, BigDecimal valor, FormaPagamento forma, SimulacaoPagamento simulacao) {
	}

	record Resposta(boolean aprovado, String transacaoId, String codigo, String mensagem) {
	}

	/** @throws GatewayIndisponivelException se o gateway estiver fora do ar ou expirar (timeout) */
	Resposta processar(Solicitacao solicitacao);

	Resposta estornar(String transacaoId, BigDecimal valor);
}
