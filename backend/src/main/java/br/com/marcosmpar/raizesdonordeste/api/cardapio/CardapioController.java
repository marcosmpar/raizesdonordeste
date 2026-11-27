package br.com.marcosmpar.raizesdonordeste.api.cardapio;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.api.comum.PaginaResponse;
import br.com.marcosmpar.raizesdonordeste.application.cardapio.CardapioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/unidades/{unidadeId}/cardapio")
@Tag(name = "Cardápio")
public class CardapioController {

	private final CardapioService service;

	public CardapioController(CardapioService service) {
		this.service = service;
	}

	@Operation(summary = "Cardápio da unidade: itens disponíveis (público, paginado)")
	@GetMapping
	public PaginaResponse<CardapioItemResponse> listar(@PathVariable Long unidadeId,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
		return PaginaResponse.de(service.listar(unidadeId, page, limit), CardapioItemResponse::de);
	}

	@Operation(summary = "Define preço/disponibilidade do produto na unidade - upsert (ADMIN/GERENTE)",
			security = @SecurityRequirement(name = "bearer-jwt"))
	@PutMapping("/{produtoId}")
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	public CardapioItemResponse definir(@PathVariable Long unidadeId, @PathVariable Long produtoId,
			@Valid @RequestBody CardapioItemRequest r) {
		return CardapioItemResponse.de(service.definir(unidadeId, produtoId, r.preco(), r.disponivel()));
	}
}
