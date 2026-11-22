package br.com.marcosmpar.raizesdonordeste.domain.estoque;

import java.util.List;

import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Saldo de um produto em uma unidade. O saldo nunca fica negativo. */
@Entity
@Table(name = "estoques")
public class Estoque {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unidade_id", nullable = false)
	private Unidade unidade;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "produto_id", nullable = false)
	private Produto produto;

	@Column(nullable = false)
	private int quantidade;

	protected Estoque() {
	}

	public Estoque(Unidade unidade, Produto produto) {
		this.unidade = unidade;
		this.produto = produto;
	}

	public void entrada(int qtd) {
		exigirPositiva(qtd);
		this.quantidade += qtd;
	}

	public void saida(int qtd) {
		exigirPositiva(qtd);
		if (qtd > quantidade) {
			throw estoqueInsuficiente(quantidade);
		}
		this.quantidade -= qtd;
	}

	public static RegraNegocioException estoqueInsuficiente(int disponivel) {
		return new RegraNegocioException("ESTOQUE_INSUFICIENTE",
				"Não há quantidade suficiente para um ou mais itens.",
				List.of(new RegraNegocioException.Detalhe("quantidade", "Disponível: " + disponivel)));
	}

	private static void exigirPositiva(int qtd) {
		if (qtd <= 0) {
			throw new IllegalArgumentException("Quantidade deve ser positiva.");
		}
	}

	public Long getId() { return id; }
	public Unidade getUnidade() { return unidade; }
	public Produto getProduto() { return produto; }
	public int getQuantidade() { return quantidade; }
}
