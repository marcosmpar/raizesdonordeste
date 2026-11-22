package br.com.marcosmpar.raizesdonordeste.api.cardapio;

import java.math.BigDecimal;

import br.com.marcosmpar.raizesdonordeste.domain.cardapio.CardapioItem;

public record CardapioItemResponse(Long produtoId, String nome, String descricao, String categoria, BigDecimal preco,
		boolean disponivel) {

	public static CardapioItemResponse de(CardapioItem c) {
		var p = c.getProduto();
		return new CardapioItemResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(), c.getPreco(),
				c.isDisponivel());
	}
}
