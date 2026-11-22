package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.marcosmpar.raizesdonordeste.domain.pagamento.Pagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pagamento.PagamentoRepository;

@Repository
class PagamentoRepositoryAdapter implements PagamentoRepository {

	private final PagamentoJpaRepository jpa;

	PagamentoRepositoryAdapter(PagamentoJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Pagamento salvar(Pagamento p) {
		return jpa.save(p);
	}

	@Override
	public Optional<Pagamento> buscarPorId(Long id) {
		return jpa.findById(id);
	}

	@Override
	public Optional<Pagamento> buscarPorChave(Long pedidoId, String chave) {
		return jpa.findByPedidoIdAndChaveIdempotencia(pedidoId, chave);
	}

	@Override
	public List<Pagamento> listarPorPedido(Long pedidoId) {
		return jpa.findByPedidoIdOrderByIdAsc(pedidoId);
	}
}
