package br.com.marcosmpar.raizesdonordeste.api.promocao;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.api.comum.PaginaResponse;
import br.com.marcosmpar.raizesdonordeste.application.promocao.PromocaoService;
import br.com.marcosmpar.raizesdonordeste.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/promocoes")
@Tag(name = "Promoções")
public class PromocaoController {

	private final PromocaoService service;

	public PromocaoController(PromocaoService service) {
		this.service = service;
	}

	@Operation(summary = "Lista promoções vigentes (público, paginado). Aplicar: campo codigoPromocao em POST /pedidos")
	@GetMapping
	public PaginaResponse<PromocaoResponse> listar(@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
		return PaginaResponse.de(service.listarVigentes(page, limit), PromocaoResponse::de);
	}

	@Operation(summary = "Cria promoção (ADMIN/GERENTE)", security = @SecurityRequirement(name = "bearer-jwt"))
	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	public ResponseEntity<PromocaoResponse> criar(@Valid @RequestBody PromocaoRequest r,
			@AuthenticationPrincipal UsuarioAutenticado atual) {
		var p = service.criar(r.codigo(), r.descricao(), r.tipo(), r.valor(), r.valorMinimoPedido(), r.unidadeId(),
				r.inicio(), r.fim(), atual.id());
		return ResponseEntity.created(URI.create("/promocoes/" + p.getId())).body(PromocaoResponse.de(p));
	}

	@Operation(summary = "Encerra a promoção (ADMIN/GERENTE)", security = @SecurityRequirement(name = "bearer-jwt"))
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	public ResponseEntity<Void> desativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado atual) {
		service.desativar(id, atual.id());
		return ResponseEntity.noContent().build();
	}
}
