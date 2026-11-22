package br.com.marcosmpar.raizesdonordeste.domain.pedido;

import java.util.Optional;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

public interface PedidoRepository {

	Pedido salvar(Pedido pedido);

	/** Carrega o pedido com seus itens. */
	Optional<Pedido> buscarPorId(Long id);

	/** Busca com bloqueio de escrita: serializa pagamento/status/cancelamento do mesmo pedido. */
	Optional<Pedido> buscarParaAtualizacao(Long id);

	/** Mais recentes primeiro. */
	Pagina<Pedido> listar(PedidoFiltro filtro, int page, int limit);
}
