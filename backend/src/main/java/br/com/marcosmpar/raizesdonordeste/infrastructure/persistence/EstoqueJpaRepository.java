package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.marcosmpar.raizesdonordeste.domain.estoque.Estoque;
import jakarta.persistence.LockModeType;

interface EstoqueJpaRepository extends JpaRepository<Estoque, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Estoque e where e.unidade.id = :unidadeId and e.produto.id = :produtoId")
	Optional<Estoque> buscarComLock(@Param("unidadeId") Long unidadeId, @Param("produtoId") Long produtoId);

	@Query(value = "select e from Estoque e join fetch e.produto p where e.unidade.id = :unidadeId order by p.nome",
			countQuery = "select count(e) from Estoque e where e.unidade.id = :unidadeId")
	Page<Estoque> porUnidade(@Param("unidadeId") Long unidadeId, Pageable pageable);
}
