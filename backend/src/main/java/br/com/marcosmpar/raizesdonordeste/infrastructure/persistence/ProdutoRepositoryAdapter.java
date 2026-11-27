package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import br.com.marcosmpar.raizesdonordeste.domain.produto.ProdutoRepository;

@Repository
class ProdutoRepositoryAdapter implements ProdutoRepository {

	private final ProdutoJpaRepository jpa;

	ProdutoRepositoryAdapter(ProdutoJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Produto salvar(Produto p) {
		return jpa.save(p);
	}

	@Override
	public Optional<Produto> buscarPorId(Long id) {
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
	public Pagina<Produto> listarAtivos(int page, int limit) {
		return PaginaMapper.pagina(jpa.findByAtivoTrue(PaginaMapper.pageable(page, limit, Sort.by("categoria", "nome"))));
	}
}
