package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.marcosmpar.raizesdonordeste.domain.estoque.MovimentacaoEstoque;

interface MovimentacaoEstoqueJpaRepository extends JpaRepository<MovimentacaoEstoque, Long> {

	@Query(value = "select m from MovimentacaoEstoque m join fetch m.produto where m.unidade.id = :unidadeId order by m.id desc",
			countQuery = "select count(m) from MovimentacaoEstoque m where m.unidade.id = :unidadeId")
	Page<MovimentacaoEstoque> porUnidade(@Param("unidadeId") Long unidadeId, Pageable pageable);

	@Query(value = "select m from MovimentacaoEstoque m join fetch m.produto where m.unidade.id = :unidadeId"
			+ " and m.produto.id = :produtoId order by m.id desc",
			countQuery = "select count(m) from MovimentacaoEstoque m where m.unidade.id = :unidadeId"
					+ " and m.produto.id = :produtoId")
	Page<MovimentacaoEstoque> porUnidadeEProduto(@Param("unidadeId") Long unidadeId,
			@Param("produtoId") Long produtoId, Pageable pageable);
}
