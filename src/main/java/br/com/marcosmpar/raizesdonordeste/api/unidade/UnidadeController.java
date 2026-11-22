package br.com.marcosmpar.raizesdonordeste.api.unidade;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.api.comum.PaginaResponse;
import br.com.marcosmpar.raizesdonordeste.application.unidade.UnidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/unidades")
@Tag(name = "Unidades")
public class UnidadeController {

	private final UnidadeService service;

	public UnidadeController(UnidadeService service) {
		this.service = service;
	}

	@Operation(summary = "Lista unidades ativas (público, paginado)")
	@GetMapping
	public PaginaResponse<UnidadeResponse> listar(@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
		return PaginaResponse.de(service.listar(true, page, limit), UnidadeResponse::de);
	}

	@Operation(summary = "Detalha uma unidade (público)")
	@GetMapping("/{id}")
	public UnidadeResponse buscar(@PathVariable Long id) {
		return UnidadeResponse.de(service.buscar(id));
	}

	@Operation(summary = "Cria unidade (ADMIN)", security = @SecurityRequirement(name = "bearer-jwt"))
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UnidadeResponse> criar(@Valid @RequestBody UnidadeRequest r) {
		var u = service.criar(r.nome(), r.cidade(), r.uf(), r.endereco(), !Boolean.FALSE.equals(r.ativa()));
		return ResponseEntity.created(URI.create("/unidades/" + u.getId())).body(UnidadeResponse.de(u));
	}

	@Operation(summary = "Atualiza unidade (ADMIN)", security = @SecurityRequirement(name = "bearer-jwt"))
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public UnidadeResponse atualizar(@PathVariable Long id, @Valid @RequestBody UnidadeRequest r) {
		return UnidadeResponse.de(service.atualizar(id, r.nome(), r.cidade(), r.uf(), r.endereco(),
				!Boolean.FALSE.equals(r.ativa())));
	}
}
