package br.com.marcosmpar.raizesdonordeste.api.pedido;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.marcosmpar.raizesdonordeste.domain.pedido.CanalPedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.Pedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.StatusPedido;

/** Visão enxuta para listagens (sem itens). */
public record PedidoResumoResponse(Long pedidoId, StatusPedido status, CanalPedido canalPedido, Long unidadeId,
		Long clienteId, BigDecimal total, Instant createdAt) {

	public static PedidoResumoResponse de(Pedido p) {
		return new PedidoResumoResponse(p.getId(), p.getStatus(), p.getCanalPedido(), p.getUnidade().getId(),
				p.getCliente() == null ? null : p.getCliente().getId(), p.getTotal(), p.getCriadoEm());
	}
}
