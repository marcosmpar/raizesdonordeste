package br.com.marcosmpar.raizesdonordeste.domain.cardapio;

import java.util.Optional;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

public interface CardapioRepository {

	CardapioItem salvar(CardapioItem item);

	Optional<CardapioItem> buscar(Long unidadeId, Long produtoId);

	/** Itens disponíveis de produtos ativos da unidade. */
	Pagina<CardapioItem> listarDisponiveis(Long unidadeId, int page, int limit);
}
