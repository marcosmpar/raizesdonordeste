package br.com.marcosmpar.raizesdonordeste.api.estoque;

import br.com.marcosmpar.raizesdonordeste.domain.estoque.Estoque;

public record EstoqueResponse(Long unidadeId, Long produtoId, String produto, int quantidade) {

	public static EstoqueResponse de(Estoque e) {
		return new EstoqueResponse(e.getUnidade().getId(), e.getProduto().getId(), e.getProduto().getNome(),
				e.getQuantidade());
	}
}
