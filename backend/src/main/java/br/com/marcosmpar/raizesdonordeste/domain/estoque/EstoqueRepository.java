package br.com.marcosmpar.raizesdonordeste.domain.estoque;

import java.util.Optional;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

public interface EstoqueRepository {

	/** Busca com bloqueio de escrita (evita corrida em saídas concorrentes). */
	Optional<Estoque> buscarParaAtualizacao(Long unidadeId, Long produtoId);

	Estoque salvar(Estoque estoque);

	Pagina<Estoque> listarPorUnidade(Long unidadeId, int page, int limit);

	MovimentacaoEstoque registrar(MovimentacaoEstoque movimentacao);

	/** produtoId opcional (null = todos os produtos da unidade). Mais recentes primeiro. */
	Pagina<MovimentacaoEstoque> listarMovimentacoes(Long unidadeId, Long produtoId, int page, int limit);
}
