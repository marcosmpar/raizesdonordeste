package br.com.marcosmpar.raizesdonordeste.domain.pagamento;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository {

	Pagamento salvar(Pagamento pagamento);

	Optional<Pagamento> buscarPorId(Long id);

	Optional<Pagamento> buscarPorChave(Long pedidoId, String chaveIdempotencia);

	/** Tentativas do pedido, da mais antiga para a mais recente. */
	List<Pagamento> listarPorPedido(Long pedidoId);
}
