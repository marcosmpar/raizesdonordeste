package br.com.marcosmpar.raizesdonordeste.application.cardapio;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcosmpar.raizesdonordeste.domain.cardapio.CardapioItem;
import br.com.marcosmpar.raizesdonordeste.domain.cardapio.CardapioRepository;
import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import br.com.marcosmpar.raizesdonordeste.domain.produto.ProdutoRepository;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.UnidadeRepository;

@Service
public class CardapioService {

	private final CardapioRepository cardapio;
	private final UnidadeRepository unidades;
	private final ProdutoRepository produtos;

	public CardapioService(CardapioRepository cardapio, UnidadeRepository unidades, ProdutoRepository produtos) {
		this.cardapio = cardapio;
		this.unidades = unidades;
		this.produtos = produtos;
	}

	@Transactional(readOnly = true)
	public Pagina<CardapioItem> listar(Long unidadeId, int page, int limit) {
		Unidade u = unidade(unidadeId);
		if (!u.isAtiva()) {
			throw new RegraNegocioException("UNIDADE_INATIVA", "A unidade não está ativa.");
		}
		return cardapio.listarDisponiveis(unidadeId, page, limit);
	}

	/** Cria ou atualiza (upsert) a oferta do produto na unidade. */
	@Transactional
	public CardapioItem definir(Long unidadeId, Long produtoId, BigDecimal preco, boolean disponivel) {
		Unidade u = unidade(unidadeId);
		Produto p = produtos.buscarPorId(produtoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));
		if (!p.isAtivo()) {
			throw new RegraNegocioException("PRODUTO_INATIVO", "O produto está inativo e não pode ser ofertado.");
		}
		CardapioItem item = cardapio.buscar(unidadeId, produtoId).orElse(null);
		if (item == null) {
			item = new CardapioItem(u, p, preco, disponivel);
		}
		else {
			item.definir(preco, disponivel);
		}
		return cardapio.salvar(item);
	}

	private Unidade unidade(Long id) {
		return unidades.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", id));
	}
}
