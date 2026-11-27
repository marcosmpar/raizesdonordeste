package br.com.marcosmpar.raizesdonordeste.api.pedido;

import br.com.marcosmpar.raizesdonordeste.domain.pedido.StatusPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusRequest(
		@Schema(example = "PRONTO", description = "Somente PRONTO ou ENTREGUE") @NotNull StatusPedido status) {
}
