package br.com.marcosmpar.raizesdonordeste.api.unidade;

import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;

public record UnidadeResponse(Long id, String nome, String cidade, String uf, String endereco, boolean ativa) {

	public static UnidadeResponse de(Unidade u) {
		return new UnidadeResponse(u.getId(), u.getNome(), u.getCidade(), u.getUf(), u.getEndereco(), u.isAtiva());
	}
}
