package br.com.marcosmpar.raizesdonordeste.api.estoque;

import java.time.Instant;

import br.com.marcosmpar.raizesdonordeste.domain.estoque.MovimentacaoEstoque;
import br.com.marcosmpar.raizesdonordeste.domain.estoque.TipoMovimentacao;

public record MovimentacaoResponse(Long id, Long unidadeId, Long produtoId, String produto, TipoMovimentacao tipo,
		int quantidade, String motivo, Long usuarioId, Instant criadoEm) {

	public static MovimentacaoResponse de(MovimentacaoEstoque m) {
		return new MovimentacaoResponse(m.getId(), m.getUnidade().getId(), m.getProduto().getId(),
				m.getProduto().getNome(), m.getTipo(), m.getQuantidade(), m.getMotivo(), m.getUsuarioId(),
				m.getCriadoEm());
	}
}
