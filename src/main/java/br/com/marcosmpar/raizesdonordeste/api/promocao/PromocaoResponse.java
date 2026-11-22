package br.com.marcosmpar.raizesdonordeste.api.promocao;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.marcosmpar.raizesdonordeste.domain.promocao.Promocao;
import br.com.marcosmpar.raizesdonordeste.domain.promocao.TipoPromocao;

public record PromocaoResponse(Long id, String codigo, String descricao, TipoPromocao tipo, BigDecimal valor,
		BigDecimal valorMinimoPedido, Long unidadeId, Instant inicio, Instant fim, boolean ativa) {

	public static PromocaoResponse de(Promocao p) {
		return new PromocaoResponse(p.getId(), p.getCodigo(), p.getDescricao(), p.getTipo(), p.getValor(),
				p.getValorMinimoPedido(), p.getUnidade() == null ? null : p.getUnidade().getId(), p.getInicio(),
				p.getFim(), p.isAtiva());
	}
}
