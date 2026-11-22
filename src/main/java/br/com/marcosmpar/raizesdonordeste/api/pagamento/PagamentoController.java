package br.com.marcosmpar.raizesdonordeste.api.pagamento;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.application.pagamento.PagamentoService;
import br.com.marcosmpar.raizesdonordeste.application.pedido.Solicitante;
import br.com.marcosmpar.raizesdonordeste.domain.pagamento.StatusPagamento;
import br.com.marcosmpar.raizesdonordeste.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@Tag(name = "Pagamentos")
@SecurityRequirement(name = "bearer-jwt")
public class PagamentoController {

	private final PagamentoService service;

	public PagamentoController(PagamentoService service) {
		this.service = service;
	}

	@Operation(summary = "Solicita pagamento ao gateway MOCK. 201 = processado (APROVADO/RECUSADO), "
			+ "200 = repetição idempotente (mesma Idempotency-Key), 202 = gateway indisponível (PENDENTE)")
	@PostMapping("/pagamentos")
	@PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
	public ResponseEntity<PagamentoResponse> pagar(@Valid @RequestBody PagamentoRequest r,
			@RequestHeader(value = "Idempotency-Key", required = false) @Size(max = 80) String chave,
			@AuthenticationPrincipal UsuarioAutenticado atual) {
		var resultado = service.pagar(solicitante(atual), r.pedidoId(), chave, r.simulacao());
		var corpo = PagamentoResponse.de(resultado.pagamento());
		if (resultado.reaproveitado()) {
			return ResponseEntity.ok(corpo);
		}
		if (resultado.pagamento().getStatus() == StatusPagamento.PENDENTE) {
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(corpo);
		}
		return ResponseEntity.created(URI.create("/pagamentos/" + corpo.pagamentoId())).body(corpo);
	}

	@Operation(summary = "Consulta um pagamento. CLIENTE só os seus")
	@GetMapping("/pagamentos/{id}")
	@PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
	public PagamentoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado atual) {
		return PagamentoResponse.de(service.buscar(id, solicitante(atual)));
	}

	@Operation(summary = "Tentativas de pagamento de um pedido (da mais antiga para a mais recente)")
	@GetMapping("/pedidos/{pedidoId}/pagamentos")
	@PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
	public List<PagamentoResponse> doPedido(@PathVariable Long pedidoId,
			@AuthenticationPrincipal UsuarioAutenticado atual) {
		return service.listarDoPedido(pedidoId, solicitante(atual)).stream().map(PagamentoResponse::de).toList();
	}

	private static Solicitante solicitante(UsuarioAutenticado atual) {
		return new Solicitante(atual.id(), atual.perfil());
	}
}
