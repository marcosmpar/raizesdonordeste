package br.com.marcosmpar.raizesdonordeste.api.pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.com.marcosmpar.raizesdonordeste.domain.pedido.CanalPedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.FormaPagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.ItemPedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.Pedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.StatusPedido;

public record PedidoResponse(Long pedidoId, StatusPedido status, CanalPedido canalPedido, Long unidadeId,
		Long clienteId, FormaPagamento formaPagamento, BigDecimal subtotal, BigDecimal descontoPromocao,
		BigDecimal descontoFidelidade, String codigoPromocao, int pontosResgatados, BigDecimal total,
		List<ItemResponse> itens, Instant createdAt) {

	public record ItemResponse(Long produtoId, String nome, int quantidade, BigDecimal precoUnitario,
			BigDecimal subtotal) {

		static ItemResponse de(ItemPedido i) {
			return new ItemResponse(i.getProduto().getId(), i.getProdutoNome(), i.getQuantidade(),
					i.getPrecoUnitario(), i.subtotal());
		}
	}

	public static PedidoResponse de(Pedido p) {
		return new PedidoResponse(p.getId(), p.getStatus(), p.getCanalPedido(), p.getUnidade().getId(),
				p.getCliente() == null ? null : p.getCliente().getId(), p.getFormaPagamento(), p.getSubtotal(),
				p.getDescontoPromocao(), p.getDescontoFidelidade(), p.getCodigoPromocao(), p.getPontosResgatados(),
				p.getTotal(),
				p.getItens().stream().map(ItemResponse::de).toList(), p.getCriadoEm());
	}
}
