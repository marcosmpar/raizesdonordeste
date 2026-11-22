package br.com.marcosmpar.raizesdonordeste.domain.promocao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
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

/**
 * Campanha promocional aplicada por código no pedido. Regra: desconto sobre o subtotal dos itens,
 * dentro da vigência, na unidade indicada (ou em todas) e respeitando o valor mínimo do pedido.
 */
@Entity
@Table(name = "promocoes")
public class Promocao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String codigo;

	@Column(nullable = false, length = 200)
	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private TipoPromocao tipo;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal valor;

	@Column(name = "valor_minimo_pedido", precision = 10, scale = 2)
	private BigDecimal valorMinimoPedido;

	/** Nulo = válida em todas as unidades. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "unidade_id")
	private Unidade unidade;

	@Column(nullable = false)
	private Instant inicio;

	@Column(nullable = false)
	private Instant fim;

	@Column(nullable = false)
	private boolean ativa = true;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm = Instant.now();

	protected Promocao() {
	}

	public Promocao(String codigo, String descricao, TipoPromocao tipo, BigDecimal valor,
			BigDecimal valorMinimoPedido, Unidade unidade, Instant inicio, Instant fim) {
		this.codigo = normalizarCodigo(codigo);
		this.descricao = descricao.trim();
		this.tipo = tipo;
		this.valor = valor;
		this.valorMinimoPedido = valorMinimoPedido;
		this.unidade = unidade;
		this.inicio = inicio;
		this.fim = fim;
	}

	public static String normalizarCodigo(String codigo) {
		return codigo.trim().toUpperCase(Locale.ROOT);
	}

	public void desativar() {
		this.ativa = false;
	}

	/** Valida as condições de uso; lança 409 PROMOCAO_INVALIDA com o motivo. */
	public void validarPara(Long unidadeId, BigDecimal subtotal, Instant agora) {
		String motivo = null;
		if (!ativa) {
			motivo = "promoção encerrada";
		}
		else if (agora.isBefore(inicio) || agora.isAfter(fim)) {
			motivo = "fora do período de vigência";
		}
		else if (unidade != null && !unidade.getId().equals(unidadeId)) {
			motivo = "não válida para esta unidade";
		}
		else if (valorMinimoPedido != null && subtotal.compareTo(valorMinimoPedido) < 0) {
			motivo = "valor mínimo do pedido: " + valorMinimoPedido;
		}
		if (motivo != null) {
			throw new RegraNegocioException("PROMOCAO_INVALIDA", "Promoção não aplicável ao pedido.",
					List.of(new RegraNegocioException.Detalhe("codigoPromocao", motivo)));
		}
	}

	/** Desconto sobre o subtotal, nunca maior que o próprio subtotal. */
	public BigDecimal calcularDesconto(BigDecimal subtotal) {
		BigDecimal desconto = switch (tipo) {
			case PERCENTUAL -> subtotal.multiply(valor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			case VALOR_FIXO -> valor;
		};
		return desconto.min(subtotal);
	}

	public Long getId() { return id; }
	public String getCodigo() { return codigo; }
	public String getDescricao() { return descricao; }
	public TipoPromocao getTipo() { return tipo; }
	public BigDecimal getValor() { return valor; }
	public BigDecimal getValorMinimoPedido() { return valorMinimoPedido; }
	public Unidade getUnidade() { return unidade; }
	public Instant getInicio() { return inicio; }
	public Instant getFim() { return fim; }
	public boolean isAtiva() { return ativa; }
}
