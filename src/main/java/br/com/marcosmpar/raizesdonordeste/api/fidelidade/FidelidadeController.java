package br.com.marcosmpar.raizesdonordeste.api.fidelidade;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.api.comum.PaginaResponse;
import br.com.marcosmpar.raizesdonordeste.application.fidelidade.FidelidadeService;
import br.com.marcosmpar.raizesdonordeste.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Programa de fidelidade do próprio cliente autenticado. */
@RestController
@RequestMapping("/fidelidade")
@Tag(name = "Fidelidade")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasRole('CLIENTE')")
public class FidelidadeController {

	private final FidelidadeService service;

	public FidelidadeController(FidelidadeService service) {
		this.service = service;
	}

	@Operation(summary = "Saldo de pontos, status do consentimento e regras do programa")
	@GetMapping
	public FidelidadeResponses.Conta conta(@AuthenticationPrincipal UsuarioAutenticado atual) {
		return FidelidadeResponses.Conta.de(service.consultar(atual.id()));
	}

	@Operation(summary = "Concede consentimento (LGPD) para acumular e resgatar pontos")
	@PostMapping("/consentimento")
	public FidelidadeResponses.Conta conceder(@AuthenticationPrincipal UsuarioAutenticado atual) {
		return FidelidadeResponses.Conta.de(service.conceder(atual.id()));
	}

	@Operation(summary = "Revoga o consentimento: para de acumular/resgatar; o saldo é preservado")
	@DeleteMapping("/consentimento")
	public FidelidadeResponses.Conta revogar(@AuthenticationPrincipal UsuarioAutenticado atual) {
		return FidelidadeResponses.Conta.de(service.revogar(atual.id()));
	}

	@Operation(summary = "Histórico de pontos (acúmulo, resgate, estorno), mais recentes primeiro")
	@GetMapping("/historico")
	public PaginaResponse<FidelidadeResponses.Movimento> historico(@AuthenticationPrincipal UsuarioAutenticado atual,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
		return PaginaResponse.de(service.historico(atual.id(), page, limit), FidelidadeResponses.Movimento::de);
	}
}
