package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.ContaFidelidade;
import jakarta.persistence.LockModeType;

interface ContaFidelidadeJpaRepository extends JpaRepository<ContaFidelidade, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from ContaFidelidade c where c.usuarioId = :id")
	Optional<ContaFidelidade> buscarComLock(@Param("id") Long id);
}
