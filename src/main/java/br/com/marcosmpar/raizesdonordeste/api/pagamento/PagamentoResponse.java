package br.com.marcosmpar.raizesdonordeste.api.pagamento;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.marcosmpar.raizesdonordeste.domain.pagamento.Pagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pagamento.StatusPagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.FormaPagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.StatusPedido;

public record PagamentoResponse(Long pagamentoId, Long pedidoId, StatusPagamento status, StatusPedido statusPedido,
		BigDecimal valor, FormaPagamento formaPagamento, Gateway gateway, Instant createdAt) {

	/** Retorno do serviço externo (mock). */
	public record Gateway(String transacaoId, String codigo, String mensagem) {
	}

	public static PagamentoResponse de(Pagamento p) {
		return new PagamentoResponse(p.getId(), p.getPedido().getId(), p.getStatus(), p.getPedido().getStatus(),
				p.getValor(), p.getFormaPagamento(),
				new Gateway(p.getTransacaoGateway(), p.getCodigoGateway(), p.getMensagem()), p.getCriadoEm());
	}
}
