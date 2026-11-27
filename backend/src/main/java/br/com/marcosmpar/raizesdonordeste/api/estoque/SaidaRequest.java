package br.com.marcosmpar.raizesdonordeste.api.estoque;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaidaRequest(
		@Schema(example = "1") @NotNull Long unidadeId,
		@Schema(example = "10") @NotNull Long produtoId,
		@Schema(example = "3") @NotNull @Min(value = 1, message = "deve ser maior que zero") Integer quantidade,
		@Schema(example = "Perda por validade") @NotBlank @Size(max = 200) String motivo) {
}
