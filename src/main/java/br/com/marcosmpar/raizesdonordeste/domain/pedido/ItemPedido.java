package br.com.marcosmpar.raizesdonordeste.domain.pedido;

import java.math.BigDecimal;

import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "itens_pedido")
public class ItemPedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pedido_id", nullable = false)
	private Pedido pedido;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "produto_id", nullable = false)
	private Produto produto;

	@Column(name = "produto_nome", nullable = false, length = 120)
	private String produtoNome;

	@Column(nullable = false)
	private int quantidade;

	@Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
	private BigDecimal precoUnitario;

	protected ItemPedido() {
	}

	ItemPedido(Pedido pedido, Produto produto, int quantidade, BigDecimal precoUnitario) {
		this.pedido = pedido;
		this.produto = produto;
		this.produtoNome = produto.getNome();
		this.quantidade = quantidade;
		this.precoUnitario = precoUnitario;
	}

	public BigDecimal subtotal() {
		return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
	}

	public Long getId() { return id; }
	public Produto getProduto() { return produto; }
	public String getProdutoNome() { return produtoNome; }
	public int getQuantidade() { return quantidade; }
	public BigDecimal getPrecoUnitario() { return precoUnitario; }
}
