package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.estoque.Estoque;
import br.com.marcosmpar.raizesdonordeste.domain.estoque.EstoqueRepository;
import br.com.marcosmpar.raizesdonordeste.domain.estoque.MovimentacaoEstoque;

@Repository
class EstoqueRepositoryAdapter implements EstoqueRepository {

	private final EstoqueJpaRepository estoques;
	private final MovimentacaoEstoqueJpaRepository movimentacoes;

	EstoqueRepositoryAdapter(EstoqueJpaRepository estoques, MovimentacaoEstoqueJpaRepository movimentacoes) {
		this.estoques = estoques;
		this.movimentacoes = movimentacoes;
	}

	@Override
	public Optional<Estoque> buscarParaAtualizacao(Long unidadeId, Long produtoId) {
		return estoques.buscarComLock(unidadeId, produtoId);
	}

	@Override
	public Estoque salvar(Estoque estoque) {
		return estoques.saveAndFlush(estoque);
	}

	@Override
	public Pagina<Estoque> listarPorUnidade(Long unidadeId, int page, int limit) {
		return PaginaMapper.pagina(estoques.porUnidade(unidadeId, PaginaMapper.pageable(page, limit)));
	}

	@Override
	public MovimentacaoEstoque registrar(MovimentacaoEstoque m) {
		return movimentacoes.save(m);
	}

	@Override
	public Pagina<MovimentacaoEstoque> listarMovimentacoes(Long unidadeId, Long produtoId, int page, int limit) {
		var pageable = PaginaMapper.pageable(page, limit);
		return PaginaMapper.pagina(produtoId == null ? movimentacoes.porUnidade(unidadeId, pageable)
				: movimentacoes.porUnidadeEProduto(unidadeId, produtoId, pageable));
	}
}
