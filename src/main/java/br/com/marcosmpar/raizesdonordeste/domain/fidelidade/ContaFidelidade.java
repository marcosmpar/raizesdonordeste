package br.com.marcosmpar.raizesdonordeste.domain.fidelidade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Conta de pontos do cliente. Só acumula/resgata com consentimento (LGPD). */
@Entity
@Table(name = "contas_fidelidade")
public class ContaFidelidade {

	/** 1 ponto a cada R$ 1,00 pago (parte inteira). */
	public static final int PONTOS_POR_REAL = 1;
	/** Cada ponto resgatado vale R$ 0,10 de desconto. */
	public static final BigDecimal VALOR_PONTO = new BigDecimal("0.10");

	@Id
	@Column(name = "usuario_id")
	private Long usuarioId;

	@Column(nullable = false)
	private boolean consentimento;

	@Column(name = "consentimento_em")
	private Instant consentimentoEm;

	@Column(name = "revogado_em")
	private Instant revogadoEm;

	@Column(name = "saldo_pontos", nullable = false)
	private int saldoPontos;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm = Instant.now();

	protected ContaFidelidade() {
	}

	public ContaFidelidade(Long usuarioId) {
		this.usuarioId = usuarioId;
	}

	public void conceder() {
		this.consentimento = true;
		this.consentimentoEm = Instant.now();
		this.revogadoEm = null;
	}

	/** Revoga: para de acumular/resgatar novos pontos; o saldo existente é preservado. */
	public void revogar() {
		this.consentimento = false;
		this.revogadoEm = Instant.now();
	}

	public void creditar(int pontos) {
		this.saldoPontos += pontos;
	}

	public void debitar(int pontos) {
		if (pontos > saldoPontos) {
			throw pontosInsuficientes(saldoPontos);
		}
		this.saldoPontos -= pontos;
	}

	public static RegraNegocioException pontosInsuficientes(int disponivel) {
		return new RegraNegocioException("PONTOS_INSUFICIENTES", "Saldo de pontos insuficiente.",
				List.of(new RegraNegocioException.Detalhe("pontosResgatar", "Disponível: " + disponivel)));
	}

	public Long getUsuarioId() { return usuarioId; }
	public boolean isConsentimento() { return consentimento; }
	public Instant getConsentimentoEm() { return consentimentoEm; }
	public Instant getRevogadoEm() { return revogadoEm; }
	public int getSaldoPontos() { return saldoPontos; }
}
