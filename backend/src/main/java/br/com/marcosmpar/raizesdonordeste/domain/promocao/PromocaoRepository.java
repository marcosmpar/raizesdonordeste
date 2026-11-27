package br.com.marcosmpar.raizesdonordeste.domain.promocao;

import java.time.Instant;
import java.util.Optional;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

public interface PromocaoRepository {

	Promocao salvar(Promocao promocao);

	Optional<Promocao> buscarPorId(Long id);

	Optional<Promocao> buscarPorCodigo(String codigo);

	boolean existePorCodigo(String codigo);

	Pagina<Promocao> listarVigentes(Instant agora, int page, int limit);
}
