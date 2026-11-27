package br.com.marcosmpar.raizesdonordeste.domain.pagamento;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.marcosmpar.raizesdonordeste.domain.pedido.FormaPagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.Pedido;
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
@Table(name = "pagamentos")
public class Pagamento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "pedido_id", nullable = false)
	private Pedido pedido;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private StatusPagamento status = StatusPagamento.PENDENTE;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal valor;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pagamento", nullable = false, length = 20)
	private FormaPagamento formaPagamento;

	@Column(name = "chave_idempotencia", length = 80)
	private String chaveIdempotencia;

	@Column(name = "transacao_gateway", length = 60)
	private String transacaoGateway;

	@Column(name = "codigo_gateway", length = 40)
	private String codigoGateway;

	@Column(length = 200)
	private String mensagem;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm = Instant.now();

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm = Instant.now();

	protected Pagamento() {
	}

	public Pagamento(Pedido pedido, String chaveIdempotencia) {
		this.pedido = pedido;
		this.valor = pedido.getTotal();
		this.formaPagamento = pedido.getFormaPagamento();
		this.chaveIdempotencia = chaveIdempotencia;
	}

	public void aprovar(String transacao, String codigo, String mensagem) {
		resolver(StatusPagamento.APROVADO, transacao, codigo, mensagem);
	}

	public void recusar(String transacao, String codigo, String mensagem) {
		resolver(StatusPagamento.RECUSADO, transacao, codigo, mensagem);
	}

	/** Gateway fora do ar/timeout: resultado desconhecido, a tentativa pode ser repetida. */
	public void manterPendente(String codigo, String mensagem) {
		resolver(StatusPagamento.PENDENTE, null, codigo, mensagem);
	}

	public void estornar(String mensagem) {
		this.status = StatusPagamento.ESTORNADO;
		this.mensagem = mensagem;
		this.atualizadoEm = Instant.now();
	}

	private void resolver(StatusPagamento novo, String transacao, String codigo, String mensagem) {
		this.status = novo;
		this.transacaoGateway = transacao;
		this.codigoGateway = codigo;
		this.mensagem = mensagem;
		this.atualizadoEm = Instant.now();
	}

	public Long getId() { return id; }
	public Pedido getPedido() { return pedido; }
	public StatusPagamento getStatus() { return status; }
	public BigDecimal getValor() { return valor; }
	public FormaPagamento getFormaPagamento() { return formaPagamento; }
	public String getChaveIdempotencia() { return chaveIdempotencia; }
	public String getTransacaoGateway() { return transacaoGateway; }
	public String getCodigoGateway() { return codigoGateway; }
	public String getMensagem() { return mensagem; }
	public Instant getCriadoEm() { return criadoEm; }
}
