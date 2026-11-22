package br.com.marcosmpar.raizesdonordeste.api.pedido;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.api.comum.PaginaResponse;
import br.com.marcosmpar.raizesdonordeste.application.pedido.PedidoService;
import br.com.marcosmpar.raizesdonordeste.application.pedido.Solicitante;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.CanalPedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.PedidoFiltro;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.StatusPedido;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import br.com.marcosmpar.raizesdonordeste.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos")
@SecurityRequirement(name = "bearer-jwt")
public class PedidoController {

	private final PedidoService service;

	public PedidoController(PedidoService service) {
		this.service = service;
	}

	@Operation(summary = "Cria pedido: valida itens/estoque, calcula total e reserva estoque (CLIENTE/ATENDENTE/GERENTE/ADMIN)")
	@PostMapping
	@PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
	public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody CriarPedidoRequest r,
			@AuthenticationPrincipal UsuarioAutenticado atual) {
		Long clienteId = r.clienteId();
		if (atual.perfil() == Perfil.CLIENTE) {
			if (clienteId != null && !clienteId.equals(atual.id())) {
				throw new AccessDeniedException("Cliente só pode criar pedidos em seu próprio nome.");
			}
			clienteId = atual.id();
		}
		var itens = r.itens().stream()
				.map(i -> new PedidoService.ItemComando(i.produtoId(), i.quantidade())).toList();
		var pedido = service.criar(atual.id(), clienteId, r.unidadeId(), r.canalPedido(), r.formaPagamento(), itens,
				r.codigoPromocao(), r.pontosResgatar() == null ? 0 : r.pontosResgatar());
		return ResponseEntity.created(URI.create("/pedidos/" + pedido.getId())).body(PedidoResponse.de(pedido));
	}

	@Operation(summary = "Lista pedidos com filtros e paginação. CLIENTE vê apenas os seus")
	@GetMapping
	public PaginaResponse<PedidoResumoResponse> listar(@RequestParam(required = false) CanalPedido canalPedido,
			@RequestParam(required = false) StatusPedido status, @RequestParam(required = false) Long unidadeId,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
			@AuthenticationPrincipal UsuarioAutenticado atual) {
		var filtro = new PedidoFiltro(canalPedido, status, unidadeId, null);
		return PaginaResponse.de(service.listar(filtro, solicitante(atual), page, limit), PedidoResumoResponse::de);
	}

	@Operation(summary = "Detalha um pedido. CLIENTE só acessa os seus")
	@GetMapping("/{id}")
	public PedidoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado atual) {
		return PedidoResponse.de(service.buscar(id, solicitante(atual)));
	}

	@Operation(summary = "Atualiza o status: EM_PREPARO → PRONTO (COZINHA/ATENDENTE/GERENTE/ADMIN) → ENTREGUE "
			+ "(ATENDENTE/GERENTE/ADMIN). Transição inválida = 409")
	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('COZINHA','ATENDENTE','GERENTE','ADMIN')")
	public PedidoResponse atualizarStatus(@PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest r,
			@AuthenticationPrincipal UsuarioAutenticado atual) {
		if (atual.perfil() == Perfil.COZINHA && r.status() != StatusPedido.PRONTO) {
			throw new AccessDeniedException("A cozinha só pode marcar o pedido como PRONTO.");
		}
		return PedidoResponse.de(service.atualizarStatus(id, r.status(), solicitante(atual)));
	}

	@Operation(summary = "Cancela o pedido, estorna pagamento aprovado e devolve o estoque. "
			+ "CLIENTE: só o próprio e só aguardando pagamento")
	@PostMapping("/{id}/cancelamento")
	@PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
	public PedidoResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado atual) {
		return PedidoResponse.de(service.cancelar(id, solicitante(atual)));
	}

	private static Solicitante solicitante(UsuarioAutenticado atual) {
		return new Solicitante(atual.id(), atual.perfil());
	}
}
