package br.com.marcosmpar.raizesdonordeste.api.promocao;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.marcosmpar.raizesdonordeste.domain.promocao.TipoPromocao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PromocaoRequest(
		@Schema(example = "VERAO15") @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,30}", message = "3 a 30 caracteres: letras, números, _ ou -") String codigo,
		@Schema(example = "15% de desconto no verão") @NotBlank @Size(max = 200) String descricao,
		@Schema(example = "PERCENTUAL") @NotNull TipoPromocao tipo,
		@Schema(example = "15", description = "Percentual (até 100) ou valor fixo em reais") @NotNull @DecimalMin(value = "0.01", message = "deve ser maior que zero") BigDecimal valor,
		@Schema(example = "40.00", description = "Opcional: subtotal mínimo do pedido") @DecimalMin("0.00") BigDecimal valorMinimoPedido,
		@Schema(example = "1", description = "Opcional: restringe a uma unidade") Long unidadeId,
		@Schema(example = "2026-12-01T00:00:00Z") @NotNull Instant inicio,
		@Schema(example = "2027-03-01T00:00:00Z") @NotNull Instant fim) {

	@AssertTrue(message = "fim deve ser posterior ao início")
	public boolean isPeriodoValido() {
		return inicio == null || fim == null || fim.isAfter(inicio);
	}

	@AssertTrue(message = "percentual não pode passar de 100")
	public boolean isPercentualValido() {
		return tipo != TipoPromocao.PERCENTUAL || valor == null || valor.compareTo(BigDecimal.valueOf(100)) <= 0;
	}
}
