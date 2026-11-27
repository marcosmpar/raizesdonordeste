package br.com.marcosmpar.raizesdonordeste.api.estoque;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.api.comum.PaginaResponse;
import br.com.marcosmpar.raizesdonordeste.application.estoque.EstoqueService;
import br.com.marcosmpar.raizesdonordeste.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/estoque")
@Tag(name = "Estoque")
@SecurityRequirement(name = "bearer-jwt")
public class EstoqueController {

	private final EstoqueService service;

	public EstoqueController(EstoqueService service) {
		this.service = service;
	}

	@Operation(summary = "Saldo de estoque por unidade (ADMIN/GERENTE/ATENDENTE)")
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
	public PaginaResponse<EstoqueResponse> saldos(@RequestParam Long unidadeId,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
		return PaginaResponse.de(service.saldos(unidadeId, page, limit), EstoqueResponse::de);
	}

	@Operation(summary = "Histórico de movimentações da unidade, mais recentes primeiro (ADMIN/GERENTE)")
	@GetMapping("/movimentacoes")
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	public PaginaResponse<MovimentacaoResponse> movimentacoes(@RequestParam Long unidadeId,
			@RequestParam(required = false) Long produtoId, @RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
		return PaginaResponse.de(service.movimentacoes(unidadeId, produtoId, page, limit), MovimentacaoResponse::de);
	}

	@Operation(summary = "Registra entrada de estoque (ADMIN/GERENTE)")
	@PostMapping("/entradas")
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	public ResponseEntity<MovimentacaoResponse> entrada(@Valid @RequestBody MovimentacaoRequest r,
			@AuthenticationPrincipal UsuarioAutenticado atual) {
		var m = service.registrarEntrada(r.unidadeId(), r.produtoId(), r.quantidade(), r.motivo(), atual.id());
		return ResponseEntity.created(URI.create("/estoque/movimentacoes?unidadeId=" + r.unidadeId()))
				.body(MovimentacaoResponse.de(m));
	}

	@Operation(summary = "Registra saída manual de estoque, ex.: perda/ajuste (ADMIN/GERENTE)")
	@PostMapping("/saidas")
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	public ResponseEntity<MovimentacaoResponse> saida(@Valid @RequestBody SaidaRequest r,
			@AuthenticationPrincipal UsuarioAutenticado atual) {
		var m = service.registrarSaida(r.unidadeId(), r.produtoId(), r.quantidade(), r.motivo(), atual.id());
		return ResponseEntity.created(URI.create("/estoque/movimentacoes?unidadeId=" + r.unidadeId()))
				.body(MovimentacaoResponse.de(m));
	}
}
