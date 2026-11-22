package br.com.marcosmpar.raizesdonordeste.domain.fidelidade;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "movimentos_pontos")
public class MovimentoPontos {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "usuario_id", nullable = false)
	private Long usuarioId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TipoMovimentoPontos tipo;

	/** Com sinal: acúmulo positivo, resgate negativo. */
	@Column(nullable = false)
	private int pontos;

	@Column(name = "pedido_id")
	private Long pedidoId;

	@Column(length = 200)
	private String descricao;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm = Instant.now();

	protected MovimentoPontos() {
	}

	public MovimentoPontos(Long usuarioId, TipoMovimentoPontos tipo, int pontos, Long pedidoId, String descricao) {
		this.usuarioId = usuarioId;
		this.tipo = tipo;
		this.pontos = pontos;
		this.pedidoId = pedidoId;
		this.descricao = descricao;
	}

	public Long getId() { return id; }
	public Long getUsuarioId() { return usuarioId; }
	public TipoMovimentoPontos getTipo() { return tipo; }
	public int getPontos() { return pontos; }
	public Long getPedidoId() { return pedidoId; }
	public String getDescricao() { return descricao; }
	public Instant getCriadoEm() { return criadoEm; }
}
