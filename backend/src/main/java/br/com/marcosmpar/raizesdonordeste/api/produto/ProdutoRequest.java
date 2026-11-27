package br.com.marcosmpar.raizesdonordeste.api.produto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProdutoRequest(
		@Schema(example = "Baião de Dois") @NotBlank @Size(max = 120) String nome,
		@Schema(example = "Arroz, feijão-de-corda, queijo coalho") @Size(max = 300) String descricao,
		@Schema(example = "Pratos") @NotBlank @Size(max = 60) String categoria,
		@Schema(example = "39.90") @NotNull @DecimalMin(value = "0.01", message = "deve ser maior que zero")
		@Digits(integer = 8, fraction = 2) BigDecimal precoBase) {
}
