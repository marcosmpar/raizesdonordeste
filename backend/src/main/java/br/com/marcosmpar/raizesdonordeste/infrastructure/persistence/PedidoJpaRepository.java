package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.marcosmpar.raizesdonordeste.domain.pedido.Pedido;
import jakarta.persistence.LockModeType;

interface PedidoJpaRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pedido p where p.id = :id")
	Optional<Pedido> buscarComLock(@Param("id") Long id);

	@EntityGraph(attributePaths = "itens")
	Optional<Pedido> findWithItensById(Long id);
}
