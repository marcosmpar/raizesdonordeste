package br.com.marcosmpar.raizesdonordeste.api.pagamento;

import br.com.marcosmpar.raizesdonordeste.domain.pagamento.SimulacaoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PagamentoRequest(
		@Schema(example = "9001") @NotNull Long pedidoId,
		@Schema(example = "APROVADO", description = "Somente gateway MOCK: resultado simulado (padrão APROVADO). INDISPONIVEL simula falha do gateway.")
		SimulacaoPagamento simulacao) {
}
