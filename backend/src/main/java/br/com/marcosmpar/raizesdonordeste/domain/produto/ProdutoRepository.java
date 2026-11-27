package br.com.marcosmpar.raizesdonordeste.domain.produto;

import java.util.Optional;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

public interface ProdutoRepository {

	Produto salvar(Produto produto);

	Optional<Produto> buscarPorId(Long id);

	boolean existePorNome(String nome);

	boolean existePorNomeDiferenteDe(String nome, Long id);

	Pagina<Produto> listarAtivos(int page, int limit);
}
