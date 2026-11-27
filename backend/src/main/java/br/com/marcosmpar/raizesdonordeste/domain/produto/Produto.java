package br.com.marcosmpar.raizesdonordeste.domain.produto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "produtos")
public class Produto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String nome;

	@Column(length = 300)
	private String descricao;

	@Column(nullable = false, length = 60)
	private String categoria;

	@Column(name = "preco_base", nullable = false, precision = 10, scale = 2)
	private BigDecimal precoBase;

	@Column(nullable = false)
	private boolean ativo = true;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm = Instant.now();

	protected Produto() {
	}

	public Produto(String nome, String descricao, String categoria, BigDecimal precoBase) {
		atualizar(nome, descricao, categoria, precoBase);
	}

	public void atualizar(String nome, String descricao, String categoria, BigDecimal precoBase) {
		this.nome = nome.trim();
		this.descricao = descricao == null ? null : descricao.trim();
		this.categoria = categoria.trim();
		this.precoBase = precoBase;
	}

	/** Exclusão lógica: preserva histórico de pedidos. */
	public void desativar() {
		this.ativo = false;
	}

	public Long getId() { return id; }
	public String getNome() { return nome; }
	public String getDescricao() { return descricao; }
	public String getCategoria() { return categoria; }
	public BigDecimal getPrecoBase() { return precoBase; }
	public boolean isAtivo() { return ativo; }
}
