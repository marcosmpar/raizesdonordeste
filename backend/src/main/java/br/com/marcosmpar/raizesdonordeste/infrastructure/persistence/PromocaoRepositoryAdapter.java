package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.promocao.Promocao;
import br.com.marcosmpar.raizesdonordeste.domain.promocao.PromocaoRepository;

@Repository
class PromocaoRepositoryAdapter implements PromocaoRepository {

	private final PromocaoJpaRepository jpa;

	PromocaoRepositoryAdapter(PromocaoJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Promocao salvar(Promocao p) {
		return jpa.save(p);
	}

	@Override
	public Optional<Promocao> buscarPorId(Long id) {
		return jpa.findById(id);
	}

	@Override
	public Optional<Promocao> buscarPorCodigo(String codigo) {
		return jpa.findByCodigo(Promocao.normalizarCodigo(codigo));
	}

	@Override
	public boolean existePorCodigo(String codigo) {
		return jpa.existsByCodigo(Promocao.normalizarCodigo(codigo));
	}

	@Override
	public Pagina<Promocao> listarVigentes(Instant agora, int page, int limit) {
		return PaginaMapper.pagina(jpa.vigentes(agora, PaginaMapper.pageable(page, limit)));
	}
}
