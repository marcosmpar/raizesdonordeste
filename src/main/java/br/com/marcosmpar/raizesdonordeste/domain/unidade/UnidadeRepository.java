package br.com.marcosmpar.raizesdonordeste.domain.unidade;

import java.util.Optional;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

public interface UnidadeRepository {

	Unidade salvar(Unidade unidade);

	Optional<Unidade> buscarPorId(Long id);

	boolean existePorNome(String nome);

	boolean existePorNomeDiferenteDe(String nome, Long id);

	Pagina<Unidade> listar(boolean apenasAtivas, int page, int limit);
}
