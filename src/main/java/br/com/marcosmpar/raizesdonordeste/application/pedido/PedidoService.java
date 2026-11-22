package br.com.marcosmpar.raizesdonordeste.application.pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcosmpar.raizesdonordeste.application.auditoria.AuditoriaService;
import br.com.marcosmpar.raizesdonordeste.application.estoque.EstoqueService;
import br.com.marcosmpar.raizesdonordeste.application.fidelidade.FidelidadeService;
import br.com.marcosmpar.raizesdonordeste.application.pagamento.PagamentoService;
import br.com.marcosmpar.raizesdonordeste.domain.cardapio.CardapioItem;
import br.com.marcosmpar.raizesdonordeste.domain.cardapio.CardapioRepository;
import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.CanalPedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.FormaPagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.Pedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.PedidoFiltro;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.PedidoRepository;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.StatusPedido;
import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import br.com.marcosmpar.raizesdonordeste.domain.produto.ProdutoRepository;
import br.com.marcosmpar.raizesdonordeste.domain.promocao.Promocao;
import br.com.marcosmpar.raizesdonordeste.domain.promocao.PromocaoRepository;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.UnidadeRepository;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.UsuarioRepository;

@Service
public class PedidoService {

	public record ItemComando(Long produtoId, int quantidade) {
	}

	private record ItemResolvido(int indice, Produto produto, int quantidade, CardapioItem oferta) {
	}

	private final PedidoRepository pedidos;
	private final UnidadeRepository unidades;
	private final ProdutoRepository produtos;
	private final CardapioRepository cardapio;
	private final UsuarioRepository usuarios;
	private final EstoqueService estoque;
	private final PagamentoService pagamentos;
	private final PromocaoRepository promocoes;
	private final FidelidadeService fidelidade;
	private final AuditoriaService auditoria;

	public PedidoService(PedidoRepository pedidos, UnidadeRepository unidades, ProdutoRepository produtos,
			CardapioRepository cardapio, UsuarioRepository usuarios, EstoqueService estoque,
			PagamentoService pagamentos, PromocaoRepository promocoes, FidelidadeService fidelidade,
			AuditoriaService auditoria) {
		this.pedidos = pedidos;
		this.unidades = unidades;
		this.produtos = produtos;
		this.cardapio = cardapio;
		this.usuarios = usuarios;
		this.estoque = estoque;
		this.pagamentos = pagamentos;
		this.promocoes = promocoes;
		this.fidelidade = fidelidade;
		this.auditoria = auditoria;
	}

	public Pedido criar(Long criadoPorId, Long clienteId, Long unidadeId, CanalPedido canal, FormaPagamento forma,
			List<ItemComando> itensSolicitados) {
		return criar(criadoPorId, clienteId, unidadeId, canal, forma, itensSolicitados, null, 0);
	}

	/**
	 * Cria o pedido: valida unidade/cliente/itens, calcula o total (preço do cardápio da unidade - promoção -
	 * resgate de pontos) e reserva o estoque. Tudo na mesma transação: qualquer falha desfaz o pedido, as
	 * baixas de estoque, o débito de pontos e a auditoria.
	 */
	@Transactional
	public Pedido criar(Long criadoPorId, Long clienteId, Long unidadeId, CanalPedido canal, FormaPagamento forma,
			List<ItemComando> itensSolicitados, String codigoPromocao, int pontosResgatar) {
		Unidade unidade = unidades.buscarPorId(unidadeId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", unidadeId));
		if (!unidade.isAtiva()) {
			throw new RegraNegocioException("UNIDADE_INATIVA", "A unidade não está ativa.");
		}
		Usuario cliente = clienteId == null ? null : cliente(clienteId);

		List<ItemResolvido> itens = resolverItens(unidadeId, itensSolicitados);

		Pedido pedido = new Pedido(cliente, unidade, canal, forma);
		itens.forEach(i -> pedido.adicionarItem(i.produto(), i.quantidade(), i.oferta().getPreco()));

		if (codigoPromocao != null && !codigoPromocao.isBlank()) {
			Promocao promocao = promocoes.buscarPorCodigo(codigoPromocao)
					.orElseThrow(() -> new RecursoNaoEncontradoException("Promoção", codigoPromocao));
			promocao.validarPara(unidadeId, pedido.getSubtotal(), Instant.now());
			pedido.aplicarPromocao(promocao, promocao.calcularDesconto(pedido.getSubtotal()));
		}
		if (pontosResgatar > 0) {
			BigDecimal desconto = fidelidade.calcularDescontoResgate(clienteId, pontosResgatar, pedido.getTotal());
			pedido.aplicarResgate(pontosResgatar, desconto);
		}
		pedidos.salvar(pedido);
		if (pontosResgatar > 0) {
			fidelidade.debitarResgate(clienteId, pontosResgatar, pedido.getId());
		}

		baixarEstoque(pedido, unidadeId, criadoPorId, itens);
		auditoria.registrar(criadoPorId, "PEDIDO_CRIADO", "PEDIDO", pedido.getId(),
				"canal=" + canal + " unidade=" + unidadeId + " itens=" + itens.size() + " total=" + pedido.getTotal()
						+ (pedido.getCodigoPromocao() != null ? " promo=" + pedido.getCodigoPromocao() : "")
						+ (pontosResgatar > 0 ? " pontos=" + pontosResgatar : ""));
		return pedido;
	}

	@Transactional(readOnly = true)
	public Pedido buscar(Long id, Solicitante solicitante) {
		Pedido p = pedidos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
		// Cliente só enxerga os próprios pedidos (404 para não revelar a existência de pedidos alheios)
		if (solicitante.perfil() == Perfil.CLIENTE
				&& (p.getCliente() == null || !p.getCliente().getId().equals(solicitante.id()))) {
			throw new RecursoNaoEncontradoException("Pedido", id);
		}
		return p;
	}

	@Transactional(readOnly = true)
	public Pagina<Pedido> listar(PedidoFiltro filtro, Solicitante solicitante, int page, int limit) {
		if (solicitante.perfil() == Perfil.CLIENTE) {
			filtro = new PedidoFiltro(filtro.canalPedido(), filtro.status(), filtro.unidadeId(), solicitante.id());
		}
		return pedidos.listar(filtro, page, limit);
	}

	/** Avanço operacional: somente PRONTO e ENTREGUE. Pagar e cancelar têm fluxos próprios. */
	@Transactional
	public Pedido atualizarStatus(Long id, StatusPedido novo, Solicitante solicitante) {
		Pedido p = pedidos.buscarParaAtualizacao(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
		if (novo != StatusPedido.PRONTO && novo != StatusPedido.ENTREGUE) {
			throw new RegraNegocioException("TRANSICAO_INVALIDA", "O status " + novo
					+ " não pode ser definido manualmente: use POST /pagamentos (pagar) ou POST /pedidos/{id}/cancelamento.");
		}
		StatusPedido anterior = p.getStatus();
		p.transitarPara(novo);
		p.getItens(); // carrega os itens para a resposta
		auditoria.registrar(solicitante.id(), "PEDIDO_STATUS_ALTERADO", "PEDIDO", id, anterior + " -> " + novo);
		return pedidos.salvar(p);
	}

	/**
	 * Cancela o pedido (antes de ficar PRONTO), estorna o pagamento aprovado e devolve o estoque reservado.
	 * CLIENTE só cancela o próprio pedido e apenas enquanto aguarda pagamento.
	 */
	@Transactional
	public Pedido cancelar(Long id, Solicitante solicitante) {
		Pedido p = pedidos.buscarParaAtualizacao(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
		if (solicitante.perfil() == Perfil.CLIENTE) {
			if (p.getCliente() == null || !p.getCliente().getId().equals(solicitante.id())) {
				throw new RecursoNaoEncontradoException("Pedido", id);
			}
			if (p.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
				throw new RegraNegocioException("CANCELAMENTO_NAO_PERMITIDO",
						"O cliente só pode cancelar pedidos que aguardam pagamento. Procure o atendimento.");
			}
		}
		StatusPedido anterior = p.getStatus();
		p.cancelar();
		if (anterior == StatusPedido.EM_PREPARO) {
			pagamentos.estornarDoPedido(p);
		}
		fidelidade.estornarDoPedido(p);
		devolverEstoque(p, solicitante.id());
		auditoria.registrar(solicitante.id(), "PEDIDO_CANCELADO", "PEDIDO", id,
				"status anterior=" + anterior + (anterior == StatusPedido.EM_PREPARO ? " (pagamento estornado)" : ""));
		return pedidos.salvar(p);
	}

	private void devolverEstoque(Pedido p, Long usuarioId) {
		p.getItens().stream().sorted(Comparator.comparing(i -> i.getProduto().getId()))
				.forEach(i -> estoque.devolver(p.getUnidade().getId(), i.getProduto().getId(), i.getQuantidade(),
						"Cancelamento do pedido #" + p.getId(), usuarioId));
	}

	private Usuario cliente(Long clienteId) {
		Usuario u = usuarios.buscarPorId(clienteId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));
		if (u.getPerfil() != Perfil.CLIENTE) {
			throw new RegraNegocioException("CLIENTE_INVALIDO", "O usuário informado não é um cliente.");
		}
		return u;
	}

	/** Une itens repetidos, valida existência (404) e oferta na unidade (409). */
	private List<ItemResolvido> resolverItens(Long unidadeId, List<ItemComando> solicitados) {
		Map<Long, Integer> agrupados = new LinkedHashMap<>();
		solicitados.forEach(i -> agrupados.merge(i.produtoId(), i.quantidade(), Integer::sum));

		List<ItemResolvido> resolvidos = new ArrayList<>();
		int indice = 0;
		for (var entrada : agrupados.entrySet()) {
			Long produtoId = entrada.getKey();
			Produto produto = produtos.buscarPorId(produtoId)
					.orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));
			CardapioItem oferta = cardapio.buscar(unidadeId, produtoId)
					.filter(c -> c.isDisponivel() && produto.isAtivo())
					.orElseThrow(() -> new RegraNegocioException("PRODUTO_INDISPONIVEL",
							"Produto não disponível nesta unidade.",
							List.of(new RegraNegocioException.Detalhe("itens[" + resolvidos.size() + "].produtoId",
									"Produto " + produtoId + " indisponível na unidade " + unidadeId))));
			resolvidos.add(new ItemResolvido(indice++, produto, entrada.getValue(), oferta));
		}
		return resolvidos;
	}

	/** Baixa em ordem de produtoId (evita deadlock entre pedidos concorrentes). */
	private void baixarEstoque(Pedido pedido, Long unidadeId, Long usuarioId, List<ItemResolvido> itens) {
		itens.stream().sorted(Comparator.comparing(i -> i.produto().getId())).forEach(i -> {
			try {
				estoque.registrarSaida(unidadeId, i.produto().getId(), i.quantidade(), "Pedido #" + pedido.getId(),
						usuarioId);
			}
			catch (RegraNegocioException e) {
				if (!"ESTOQUE_INSUFICIENTE".equals(e.getCode())) {
					throw e;
				}
				String issue = e.getDetalhes().isEmpty() ? "indisponível" : e.getDetalhes().get(0).issue();
				throw new RegraNegocioException(e.getCode(), e.getMessage(), List.of(
						new RegraNegocioException.Detalhe("itens[" + i.indice() + "].quantidade", issue)));
			}
		});
	}
}
