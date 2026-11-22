package br.com.marcosmpar.raizesdonordeste.application.estoque;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.estoque.Estoque;
import br.com.marcosmpar.raizesdonordeste.domain.estoque.EstoqueRepository;
import br.com.marcosmpar.raizesdonordeste.domain.estoque.MovimentacaoEstoque;
import br.com.marcosmpar.raizesdonordeste.domain.estoque.TipoMovimentacao;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import br.com.marcosmpar.raizesdonordeste.domain.produto.ProdutoRepository;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.UnidadeRepository;

/**
 * Controle de estoque por unidade. {@link #registrarSaida} também será usada pelo fluxo de pedidos
 * para restringir a venda por indisponibilidade.
 */
@Service
public class EstoqueService {

	private final EstoqueRepository estoques;
	private final UnidadeRepository unidades;
	private final ProdutoRepository produtos;

	public EstoqueService(EstoqueRepository estoques, UnidadeRepository unidades, ProdutoRepository produtos) {
		this.estoques = estoques;
		this.unidades = unidades;
		this.produtos = produtos;
	}

	@Transactional
	public MovimentacaoEstoque registrarEntrada(Long unidadeId, Long produtoId, int quantidade, String motivo,
			Long usuarioId) {
		return entrada(unidade(unidadeId), produtoAtivo(produtoId), quantidade, motivo, usuarioId);
	}

	/** Devolve ao estoque itens reservados (cancelamento). Não exige produto ativo. */
	@Transactional
	public MovimentacaoEstoque devolver(Long unidadeId, Long produtoId, int quantidade, String motivo,
			Long usuarioId) {
		return entrada(unidade(unidadeId), produto(produtoId), quantidade, motivo, usuarioId);
	}

	private MovimentacaoEstoque entrada(Unidade unidade, Produto produto, int quantidade, String motivo,
			Long usuarioId) {
		Long unidadeId = unidade.getId();
		Long produtoId = produto.getId();
		Estoque estoque = estoques.buscarParaAtualizacao(unidadeId, produtoId)
				.orElseGet(() -> new Estoque(unidade, produto));
		estoque.entrada(quantidade);
		estoques.salvar(estoque);
		return estoques.registrar(
				new MovimentacaoEstoque(unidade, produto, TipoMovimentacao.ENTRADA, quantidade, motivo, usuarioId));
	}

	@Transactional
	public MovimentacaoEstoque registrarSaida(Long unidadeId, Long produtoId, int quantidade, String motivo,
			Long usuarioId) {
		Unidade unidade = unidade(unidadeId);
		Produto produto = produto(produtoId);
		Estoque estoque = estoques.buscarParaAtualizacao(unidadeId, produtoId)
				.orElseThrow(() -> Estoque.estoqueInsuficiente(0));
		estoque.saida(quantidade); // lança ESTOQUE_INSUFICIENTE (409) se faltar saldo
		estoques.salvar(estoque);
		return estoques.registrar(
				new MovimentacaoEstoque(unidade, produto, TipoMovimentacao.SAIDA, quantidade, motivo, usuarioId));
	}

	@Transactional(readOnly = true)
	public Pagina<Estoque> saldos(Long unidadeId, int page, int limit) {
		unidade(unidadeId);
		return estoques.listarPorUnidade(unidadeId, page, limit);
	}

	@Transactional(readOnly = true)
	public Pagina<MovimentacaoEstoque> movimentacoes(Long unidadeId, Long produtoId, int page, int limit) {
		unidade(unidadeId);
		if (produtoId != null) {
			produto(produtoId);
		}
		return estoques.listarMovimentacoes(unidadeId, produtoId, page, limit);
	}

	private Unidade unidade(Long id) {
		return unidades.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", id));
	}

	private Produto produto(Long id) {
		return produtos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
	}

	private Produto produtoAtivo(Long id) {
		Produto p = produto(id);
		if (!p.isAtivo()) {
			throw new RegraNegocioException("PRODUTO_INATIVO", "O produto está inativo.");
		}
		return p;
	}
}
