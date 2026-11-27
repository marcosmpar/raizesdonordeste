package br.com.marcosmpar.raizesdonordeste.domain.unidade;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "unidades")
public class Unidade {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String nome;

	@Column(nullable = false, length = 80)
	private String cidade;

	@Column(nullable = false, length = 2)
	private String uf;

	@Column(nullable = false, length = 200)
	private String endereco;

	@Column(nullable = false)
	private boolean ativa = true;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm = Instant.now();

	protected Unidade() {
	}

	public Unidade(String nome, String cidade, String uf, String endereco, boolean ativa) {
		atualizar(nome, cidade, uf, endereco, ativa);
	}

	public void atualizar(String nome, String cidade, String uf, String endereco, boolean ativa) {
		this.nome = nome.trim();
		this.cidade = cidade.trim();
		this.uf = uf.trim().toUpperCase();
		this.endereco = endereco.trim();
		this.ativa = ativa;
	}

	public Long getId() { return id; }
	public String getNome() { return nome; }
	public String getCidade() { return cidade; }
	public String getUf() { return uf; }
	public String getEndereco() { return endereco; }
	public boolean isAtiva() { return ativa; }
}
