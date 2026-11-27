package br.com.marcosmpar.raizesdonordeste.api.pedido;

import java.util.List;

import br.com.marcosmpar.raizesdonordeste.domain.pedido.CanalPedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.FormaPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarPedidoRequest(
		@Schema(example = "TOTEM", description = "Canal de origem (obrigatório)") @NotNull CanalPedido canalPedido,
		@Schema(example = "12", description = "Opcional. CLIENTE usa o próprio id; equipe pode informar o cliente.") Long clienteId,
		@Schema(example = "1") @NotNull Long unidadeId,
		@NotEmpty List<@Valid ItemRequest> itens,
		@Schema(example = "MOCK") @NotNull FormaPagamento formaPagamento,
		@Schema(example = "BEMVINDO10", description = "Opcional: código de promoção") @Size(max = 30) String codigoPromocao,
		@Schema(example = "30", description = "Opcional: pontos de fidelidade a resgatar (1 ponto = R$ 0,10). Exige consentimento.")
		@Min(0) Integer pontosResgatar) {

	public record ItemRequest(
			@Schema(example = "10") @NotNull Long produtoId,
			@Schema(example = "2") @NotNull @Min(value = 1, message = "deve ser maior que zero") @Max(99) Integer quantidade) {
	}
}
