package br.com.marcosmpar.raizesdonordeste.api.cardapio;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record CardapioItemRequest(
		@Schema(example = "42.50") @NotNull @DecimalMin(value = "0.01", message = "deve ser maior que zero")
		@Digits(integer = 8, fraction = 2) BigDecimal preco,
		@Schema(example = "true") @NotNull Boolean disponivel) {
}
