package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.marcosmpar.raizesdonordeste.domain.cardapio.CardapioItem;

interface CardapioJpaRepository extends JpaRepository<CardapioItem, Long> {

	@Query("select c from CardapioItem c join fetch c.produto where c.unidade.id = :unidadeId and c.produto.id = :produtoId")
	Optional<CardapioItem> buscar(@Param("unidadeId") Long unidadeId, @Param("produtoId") Long produtoId);

	@Query(value = "select c from CardapioItem c join fetch c.produto p where c.unidade.id = :unidadeId"
			+ " and c.disponivel = true and p.ativo = true order by p.categoria, p.nome",
			countQuery = "select count(c) from CardapioItem c where c.unidade.id = :unidadeId"
					+ " and c.disponivel = true and c.produto.ativo = true")
	Page<CardapioItem> disponiveis(@Param("unidadeId") Long unidadeId, Pageable pageable);
}
