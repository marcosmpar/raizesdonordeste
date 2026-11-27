package br.com.marcosmpar.raizesdonordeste.application.produto;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import br.com.marcosmpar.raizesdonordeste.domain.produto.ProdutoRepository;

@Service
public class ProdutoService {

	private final ProdutoRepository produtos;

	public ProdutoService(ProdutoRepository produtos) {
		this.produtos = produtos;
	}

	@Transactional
	public Produto criar(String nome, String descricao, String categoria, BigDecimal precoBase) {
		if (produtos.existePorNome(nome)) {
			throw new RegraNegocioException("PRODUTO_JA_CADASTRADO", "Já existe um produto com este nome.");
		}
		return produtos.salvar(new Produto(nome, descricao, categoria, precoBase));
	}

	@Transactional
	public Produto atualizar(Long id, String nome, String descricao, String categoria, BigDecimal precoBase) {
		Produto p = buscar(id);
		if (produtos.existePorNomeDiferenteDe(nome, id)) {
			throw new RegraNegocioException("PRODUTO_JA_CADASTRADO", "Já existe um produto com este nome.");
		}
		p.atualizar(nome, descricao, categoria, precoBase);
		return produtos.salvar(p);
	}

	@Transactional
	public void desativar(Long id) {
		Produto p = buscar(id);
		p.desativar();
		produtos.salvar(p);
	}

	@Transactional(readOnly = true)
	public Produto buscar(Long id) {
		return produtos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
	}

	@Transactional(readOnly = true)
	public Pagina<Produto> listar(int page, int limit) {
		return produtos.listarAtivos(page, limit);
	}
}
