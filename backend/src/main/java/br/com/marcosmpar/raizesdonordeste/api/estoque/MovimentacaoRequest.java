package br.com.marcosmpar.raizesdonordeste.api.estoque;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimentacaoRequest(
		@Schema(example = "1") @NotNull Long unidadeId,
		@Schema(example = "10") @NotNull Long produtoId,
		@Schema(example = "50") @NotNull @Min(value = 1, message = "deve ser maior que zero") Integer quantidade,
		@Schema(example = "Reposição semanal") @Size(max = 200) String motivo) {
}
