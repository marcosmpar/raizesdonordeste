package br.com.marcosmpar.raizesdonordeste.domain.pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import br.com.marcosmpar.raizesdonordeste.domain.promocao.Promocao;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedidos")
public class Pedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Opcional: pedidos de balcão/totem podem não ter cliente identificado. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id")
	private Usuario cliente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unidade_id", nullable = false)
	private Unidade unidade;

	@Enumerated(EnumType.STRING)
	@Column(name = "canal_pedido", nullable = false, length = 10)
	private CanalPedido canalPedido;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 25)
	private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pagamento", nullable = false, length = 20)
	private FormaPagamento formaPagamento;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal = BigDecimal.ZERO;

	@Column(name = "desconto_promocao", nullable = false, precision = 12, scale = 2)
	private BigDecimal descontoPromocao = BigDecimal.ZERO;

	@Column(name = "desconto_fidelidade", nullable = false, precision = 12, scale = 2)
	private BigDecimal descontoFidelidade = BigDecimal.ZERO;

	@Column(name = "pontos_resgatados", nullable = false)
	private int pontosResgatados;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "promocao_id")
	private Promocao promocao;

	/** Cópia do código usado (o histórico não depende de a campanha continuar existindo). */
	@Column(name = "codigo_promocao", length = 30)
	private String codigoPromocao;

	/** total = subtotal - descontos (promoção, depois fidelidade). */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total = BigDecimal.ZERO;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm = Instant.now();

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm = Instant.now();

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ItemPedido> itens = new ArrayList<>();

	protected Pedido() {
	}

	public Pedido(Usuario cliente, Unidade unidade, CanalPedido canalPedido, FormaPagamento formaPagamento) {
		this.cliente = cliente;
		this.unidade = unidade;
		this.canalPedido = canalPedido;
		this.formaPagamento = formaPagamento;
	}

	/** Adiciona item com o preço vigente na unidade e recalcula o total (calculado sempre no servidor). */
	public void adicionarItem(Produto produto, int quantidade, BigDecimal precoUnitario) {
		ItemPedido item = new ItemPedido(this, produto, quantidade, precoUnitario);
		itens.add(item);
		recalcular();
	}

	public void aplicarPromocao(Promocao promocao, BigDecimal desconto) {
		this.promocao = promocao;
		this.codigoPromocao = promocao.getCodigo();
		this.descontoPromocao = desconto;
		recalcular();
	}

	public void aplicarResgate(int pontos, BigDecimal desconto) {
		this.pontosResgatados = pontos;
		this.descontoFidelidade = desconto;
		recalcular();
	}

	private void recalcular() {
		this.subtotal = itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		this.total = subtotal.subtract(descontoPromocao).subtract(descontoFidelidade).max(BigDecimal.ZERO);
	}

	/** Aplica a transição respeitando a máquina de estados (senão 409 TRANSICAO_INVALIDA). */
	public void transitarPara(StatusPedido destino) {
		if (!status.podeIrPara(destino)) {
			throw new RegraNegocioException("TRANSICAO_INVALIDA",
					"Não é possível mudar o pedido de " + status + " para " + destino + ".",
					List.of(new RegraNegocioException.Detalhe("status", "Status atual: " + status)));
		}
		this.status = destino;
		this.atualizadoEm = Instant.now();
	}

	public void confirmarPagamento() {
		transitarPara(StatusPedido.EM_PREPARO);
	}

	public void cancelar() {
		transitarPara(StatusPedido.CANCELADO);
	}

	public Long getId() { return id; }
	public Usuario getCliente() { return cliente; }
	public Unidade getUnidade() { return unidade; }
	public CanalPedido getCanalPedido() { return canalPedido; }
	public StatusPedido getStatus() { return status; }
	public FormaPagamento getFormaPagamento() { return formaPagamento; }
	public BigDecimal getSubtotal() { return subtotal; }
	public BigDecimal getDescontoPromocao() { return descontoPromocao; }
	public BigDecimal getDescontoFidelidade() { return descontoFidelidade; }
	public int getPontosResgatados() { return pontosResgatados; }
	public String getCodigoPromocao() { return codigoPromocao; }
	public BigDecimal getTotal() { return total; }
	public Instant getCriadoEm() { return criadoEm; }
	public Instant getAtualizadoEm() { return atualizadoEm; }
	public List<ItemPedido> getItens() { return List.copyOf(itens); }
}
