package br.com.marcosmpar.raizesdonordeste.infrastructure.pagamento;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.marcosmpar.raizesdonordeste.application.pagamento.GatewayIndisponivelException;
import br.com.marcosmpar.raizesdonordeste.application.pagamento.GatewayPagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pagamento.SimulacaoPagamento;

/** Gateway de pagamento SIMULADO: nenhuma cobrança real. O resultado vem de {@code simulacao}. */
@Component
public class GatewayPagamentoMock implements GatewayPagamento {

	private static final Logger log = LoggerFactory.getLogger(GatewayPagamentoMock.class);

	@Override
	public Resposta processar(Solicitacao s) {
		SimulacaoPagamento simulacao = s.simulacao() == null ? SimulacaoPagamento.APROVADO : s.simulacao();
		log.info("[gateway-mock] pedido={} valor={} forma={} simulacao={}", s.pedidoId(), s.valor(), s.forma(),
				simulacao);
		return switch (simulacao) {
			case APROVADO -> new Resposta(true, novaTransacao(), "APPROVED", "Pagamento aprovado (simulado).");
			case RECUSADO -> new Resposta(false, novaTransacao(), "DECLINED",
					"Pagamento recusado pelo emissor (simulado).");
			case INDISPONIVEL -> throw new GatewayIndisponivelException("Gateway de pagamento indisponível (simulado).");
		};
	}

	@Override
	public Resposta estornar(String transacaoId, BigDecimal valor) {
		log.info("[gateway-mock] estorno transacao={} valor={}", transacaoId, valor);
		return new Resposta(true, transacaoId, "REFUNDED", "Estorno realizado (simulado).");
	}

	private static String novaTransacao() {
		return "MOCK-" + UUID.randomUUID();
	}
}
