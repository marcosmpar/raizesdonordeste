package br.com.marcosmpar.raizesdonordeste.api.unidade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UnidadeRequest(
		@Schema(example = "Raízes - Recife Antigo") @NotBlank @Size(max = 120) String nome,
		@Schema(example = "Recife") @NotBlank @Size(max = 80) String cidade,
		@Schema(example = "PE") @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "deve ter 2 letras") String uf,
		@Schema(example = "Rua do Bom Jesus, 100") @NotBlank @Size(max = 200) String endereco,
		@Schema(example = "true", description = "Padrão: true") Boolean ativa) {
}
