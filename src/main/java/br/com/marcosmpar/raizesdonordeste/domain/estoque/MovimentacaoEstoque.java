package br.com.marcosmpar.raizesdonordeste.domain.estoque;

import java.time.Instant;

import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoque {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unidade_id", nullable = false)
	private Unidade unidade;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "produto_id", nullable = false)
	private Produto produto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TipoMovimentacao tipo;

	@Column(nullable = false)
	private int quantidade;

	@Column(length = 200)
	private String motivo;

	@Column(name = "usuario_id")
	private Long usuarioId;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm = Instant.now();

	protected MovimentacaoEstoque() {
	}

	public MovimentacaoEstoque(Unidade unidade, Produto produto, TipoMovimentacao tipo, int quantidade,
			String motivo, Long usuarioId) {
		this.unidade = unidade;
		this.produto = produto;
		this.tipo = tipo;
		this.quantidade = quantidade;
		this.motivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
		this.usuarioId = usuarioId;
	}

	public Long getId() { return id; }
	public Unidade getUnidade() { return unidade; }
	public Produto getProduto() { return produto; }
	public TipoMovimentacao getTipo() { return tipo; }
	public int getQuantidade() { return quantidade; }
	public String getMotivo() { return motivo; }
	public Long getUsuarioId() { return usuarioId; }
	public Instant getCriadoEm() { return criadoEm; }
}
