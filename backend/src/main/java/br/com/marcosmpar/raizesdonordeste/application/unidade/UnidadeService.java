package br.com.marcosmpar.raizesdonordeste.application.unidade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.UnidadeRepository;

@Service
public class UnidadeService {

	private final UnidadeRepository unidades;

	public UnidadeService(UnidadeRepository unidades) {
		this.unidades = unidades;
	}

	@Transactional
	public Unidade criar(String nome, String cidade, String uf, String endereco, boolean ativa) {
		if (unidades.existePorNome(nome)) {
			throw new RegraNegocioException("UNIDADE_JA_CADASTRADA", "Já existe uma unidade com este nome.");
		}
		return unidades.salvar(new Unidade(nome, cidade, uf, endereco, ativa));
	}

	@Transactional
	public Unidade atualizar(Long id, String nome, String cidade, String uf, String endereco, boolean ativa) {
		Unidade u = buscar(id);
		if (unidades.existePorNomeDiferenteDe(nome, id)) {
			throw new RegraNegocioException("UNIDADE_JA_CADASTRADA", "Já existe uma unidade com este nome.");
		}
		u.atualizar(nome, cidade, uf, endereco, ativa);
		return unidades.salvar(u);
	}

	@Transactional(readOnly = true)
	public Unidade buscar(Long id) {
		return unidades.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", id));
	}

	@Transactional(readOnly = true)
	public Pagina<Unidade> listar(boolean apenasAtivas, int page, int limit) {
		return unidades.listar(apenasAtivas, page, limit);
	}
}
