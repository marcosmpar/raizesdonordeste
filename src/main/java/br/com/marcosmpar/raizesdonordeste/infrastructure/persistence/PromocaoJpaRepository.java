package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.marcosmpar.raizesdonordeste.domain.promocao.Promocao;

interface PromocaoJpaRepository extends JpaRepository<Promocao, Long> {

	Optional<Promocao> findByCodigo(String codigo);

	boolean existsByCodigo(String codigo);

	@Query(value = "select p from Promocao p where p.ativa = true and p.inicio <= :agora and p.fim >= :agora order by p.fim",
			countQuery = "select count(p) from Promocao p where p.ativa = true and p.inicio <= :agora and p.fim >= :agora")
	Page<Promocao> vigentes(@Param("agora") Instant agora, Pageable pageable);
}
