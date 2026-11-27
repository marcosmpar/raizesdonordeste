package br.com.marcosmpar.raizesdonordeste.api.produto;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.api.comum.PaginaResponse;
import br.com.marcosmpar.raizesdonordeste.application.produto.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/produtos")
@Tag(name = "Produtos")
public class ProdutoController {

	private final ProdutoService service;

	public ProdutoController(ProdutoService service) {
		this.service = service;
	}

	@Operation(summary = "Lista produtos ativos (público, paginado)")
	@GetMapping
	public PaginaResponse<ProdutoResponse> listar(@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
		return PaginaResponse.de(service.listar(page, limit), ProdutoResponse::de);
	}

	@Operation(summary = "Detalha um produto (público)")
	@GetMapping("/{id}")
	public ProdutoResponse buscar(@PathVariable Long id) {
		return ProdutoResponse.de(service.buscar(id));
	}

	@Operation(summary = "Cria produto (ADMIN/GERENTE)", security = @SecurityRequirement(name = "bearer-jwt"))
	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest r) {
		var p = service.criar(r.nome(), r.descricao(), r.categoria(), r.precoBase());
		return ResponseEntity.created(URI.create("/produtos/" + p.getId())).body(ProdutoResponse.de(p));
	}

	@Operation(summary = "Atualiza produto (ADMIN/GERENTE)", security = @SecurityRequirement(name = "bearer-jwt"))
	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest r) {
		return ProdutoResponse.de(service.atualizar(id, r.nome(), r.descricao(), r.categoria(), r.precoBase()));
	}

	@Operation(summary = "Desativa produto - exclusão lógica (ADMIN/GERENTE)",
			security = @SecurityRequirement(name = "bearer-jwt"))
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
	public ResponseEntity<Void> desativar(@PathVariable Long id) {
		service.desativar(id);
		return ResponseEntity.noContent().build();
	}
}
