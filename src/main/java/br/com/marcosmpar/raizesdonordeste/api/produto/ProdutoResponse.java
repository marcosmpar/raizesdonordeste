package br.com.marcosmpar.raizesdonordeste.api.produto;

import java.math.BigDecimal;

import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;

public record ProdutoResponse(Long id, String nome, String descricao, String categoria, BigDecimal precoBase,
		boolean ativo) {

	public static ProdutoResponse de(Produto p) {
		return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(), p.getPrecoBase(),
				p.isAtivo());
	}
}
