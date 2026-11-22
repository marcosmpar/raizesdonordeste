package br.com.marcosmpar.raizesdonordeste.api.error;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Corpo padrão de TODAS as respostas de erro da API. */
@Schema(description = "Padrão de erro da API")
public record ErroResponse(
		@Schema(example = "ESTOQUE_INSUFICIENTE") String error,
		@Schema(example = "Não há quantidade suficiente para um ou mais itens.") String message,
		List<Detalhe> details,
		@Schema(example = "2026-02-05T12:00:00Z") Instant timestamp,
		@Schema(example = "/pedidos") String path,
		@Schema(example = "3f6c2c9e-6a1e-4e0b-9d57-0c1d7a2b5f10") String requestId) {

	public record Detalhe(
			@Schema(example = "itens[0].quantidade") String field,
			@Schema(example = "Disponível: 1") String issue) {
	}
}
