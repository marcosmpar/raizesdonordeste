package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.marcosmpar.raizesdonordeste.domain.cardapio.CardapioItem;
import br.com.marcosmpar.raizesdonordeste.domain.cardapio.CardapioRepository;
import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

@Repository
class CardapioRepositoryAdapter implements CardapioRepository {

	private final CardapioJpaRepository jpa;

	CardapioRepositoryAdapter(CardapioJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public CardapioItem salvar(CardapioItem item) {
		return jpa.save(item);
	}

	@Override
	public Optional<CardapioItem> buscar(Long unidadeId, Long produtoId) {
		return jpa.buscar(unidadeId, produtoId);
	}

	@Override
	public Pagina<CardapioItem> listarDisponiveis(Long unidadeId, int page, int limit) {
		return PaginaMapper.pagina(jpa.disponiveis(unidadeId, PaginaMapper.pageable(page, limit)));
	}
}
