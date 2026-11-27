package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.Pedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.PedidoFiltro;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.PedidoRepository;

@Repository
class PedidoRepositoryAdapter implements PedidoRepository {

	private final PedidoJpaRepository jpa;

	PedidoRepositoryAdapter(PedidoJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Pedido salvar(Pedido pedido) {
		return jpa.save(pedido);
	}

	@Override
	public Optional<Pedido> buscarPorId(Long id) {
		return jpa.findWithItensById(id);
	}

	@Override
	public Optional<Pedido> buscarParaAtualizacao(Long id) {
		return jpa.buscarComLock(id);
	}

	@Override
	public Pagina<Pedido> listar(PedidoFiltro f, int page, int limit) {
		Specification<Pedido> spec = (root, query, cb) -> cb.conjunction();
		if (f.canalPedido() != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("canalPedido"), f.canalPedido()));
		}
		if (f.status() != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), f.status()));
		}
		if (f.unidadeId() != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("unidade").get("id"), f.unidadeId()));
		}
		if (f.clienteId() != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("cliente").get("id"), f.clienteId()));
		}
		return PaginaMapper.pagina(jpa.findAll(spec, PaginaMapper.pageable(page, limit, Sort.by(Sort.Direction.DESC, "id"))));
	}
}
