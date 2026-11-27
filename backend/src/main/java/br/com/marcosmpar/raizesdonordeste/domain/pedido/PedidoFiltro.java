package br.com.marcosmpar.raizesdonordeste.domain.pedido;

/** Filtros opcionais de listagem (campos nulos são ignorados). */
public record PedidoFiltro(CanalPedido canalPedido, StatusPedido status, Long unidadeId, Long clienteId) {
}
