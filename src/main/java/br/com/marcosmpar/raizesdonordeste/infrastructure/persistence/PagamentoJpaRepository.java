package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.marcosmpar.raizesdonordeste.domain.pagamento.Pagamento;

interface PagamentoJpaRepository extends JpaRepository<Pagamento, Long> {

	Optional<Pagamento> findByPedidoIdAndChaveIdempotencia(Long pedidoId, String chaveIdempotencia);

	List<Pagamento> findByPedidoIdOrderByIdAsc(Long pedidoId);
}
