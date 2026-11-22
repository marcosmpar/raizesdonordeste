package br.com.marcosmpar.raizesdonordeste.domain.cardapio;

import java.math.BigDecimal;

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

/** Oferta de um produto em uma unidade (preço e disponibilidade próprios). */
@Entity
@Table(name = "cardapio_itens")
public class CardapioItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unidade_id", nullable = false)
	private Unidade unidade;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "produto_id", nullable = false)
	private Produto produto;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal preco;

	@Column(nullable = false)
	private boolean disponivel = true;

	protected CardapioItem() {
	}

	public CardapioItem(Unidade unidade, Produto produto, BigDecimal preco, boolean disponivel) {
		this.unidade = unidade;
		this.produto = produto;
		definir(preco, disponivel);
	}

	public void definir(BigDecimal preco, boolean disponivel) {
		this.preco = preco;
		this.disponivel = disponivel;
	}

	public Long getId() { return id; }
	public Unidade getUnidade() { return unidade; }
	public Produto getProduto() { return produto; }
	public BigDecimal getPreco() { return preco; }
	public boolean isDisponivel() { return disponivel; }
}
