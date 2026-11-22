package br.com.marcosmpar.raizesdonordeste.api.auditoria;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.api.comum.PaginaResponse;
import br.com.marcosmpar.raizesdonordeste.application.auditoria.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/auditoria")
@Tag(name = "Auditoria")
@SecurityRequirement(name = "bearer-jwt")
public class AuditoriaController {

	private final AuditoriaService service;

	public AuditoriaController(AuditoriaService service) {
		this.service = service;
	}

	@Operation(summary = "Consulta o log de auditoria de ações sensíveis (ADMIN/GERENTE). "
			+ "Entidades: PEDIDO, PAGAMENTO, USUARIO, PROMOCAO, CONTA_FIDELIDADE")
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	public PaginaResponse<AuditoriaResponse> listar(@RequestParam(required = false) String entidade,
			@RequestParam(required = false) Long entidadeId, @RequestParam(required = false) String acao,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
		return PaginaResponse.de(service.listar(entidade, entidadeId, acao, page, limit), AuditoriaResponse::de);
	}
}
