package br.com.marcosmpar.raizesdonordeste.api.fidelidade;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.ContaFidelidade;
import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.MovimentoPontos;
import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.TipoMovimentoPontos;

public final class FidelidadeResponses {

	private FidelidadeResponses() {
	}

	public record Regras(int pontosPorReal, BigDecimal valorPontoEmReais) {
	}

	public record Conta(boolean consentimento, Instant consentimentoEm, Instant revogadoEm, int saldoPontos,
			Regras regras) {

		public static Conta de(ContaFidelidade c) {
			return new Conta(c.isConsentimento(), c.getConsentimentoEm(), c.getRevogadoEm(), c.getSaldoPontos(),
					new Regras(ContaFidelidade.PONTOS_POR_REAL, ContaFidelidade.VALOR_PONTO));
		}
	}

	public record Movimento(Long id, TipoMovimentoPontos tipo, int pontos, Long pedidoId, String descricao,
			Instant createdAt) {

		public static Movimento de(MovimentoPontos m) {
			return new Movimento(m.getId(), m.getTipo(), m.getPontos(), m.getPedidoId(), m.getDescricao(),
					m.getCriadoEm());
		}
	}
}
