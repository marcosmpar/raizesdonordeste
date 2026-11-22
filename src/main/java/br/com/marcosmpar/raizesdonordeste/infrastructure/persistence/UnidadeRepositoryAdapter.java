package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.UnidadeRepository;

@Repository
class UnidadeRepositoryAdapter implements UnidadeRepository {

	private final UnidadeJpaRepository jpa;

	UnidadeRepositoryAdapter(UnidadeJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Unidade salvar(Unidade u) {
		return jpa.save(u);
	}

	@Override
	public Optional<Unidade> buscarPorId(Long id) {
		return jpa.findById(id);
	}

	@Override
	public boolean existePorNome(String nome) {
		return jpa.existsByNomeIgnoreCase(nome.trim());
	}

	@Override
	public boolean existePorNomeDiferenteDe(String nome, Long id) {
		return jpa.existsByNomeIgnoreCaseAndIdNot(nome.trim(), id);
	}

	@Override
	public Pagina<Unidade> listar(boolean apenasAtivas, int page, int limit) {
		var pageable = PaginaMapper.pageable(page, limit, Sort.by("nome"));
		return PaginaMapper.pagina(apenasAtivas ? jpa.findByAtiva(true, pageable) : jpa.findAll(pageable));
	}
}
