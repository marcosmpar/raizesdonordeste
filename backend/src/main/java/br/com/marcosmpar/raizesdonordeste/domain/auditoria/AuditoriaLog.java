package br.com.marcosmpar.raizesdonordeste.domain.auditoria;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Registro imutável de uma ação sensível. Não guarda dados pessoais nem segredos. */
@Entity
@Table(name = "auditoria_logs")
public class AuditoriaLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "ator_id")
	private Long atorId;

	@Column(nullable = false, length = 40)
	private String acao;

	@Column(nullable = false, length = 40)
	private String entidade;

	@Column(name = "entidade_id")
	private Long entidadeId;

	@Column(length = 500)
	private String detalhes;

	@Column(name = "request_id", length = 60)
	private String requestId;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm = Instant.now();

	protected AuditoriaLog() {
	}

	public AuditoriaLog(Long atorId, String acao, String entidade, Long entidadeId, String detalhes,
			String requestId) {
		this.atorId = atorId;
		this.acao = acao;
		this.entidade = entidade;
		this.entidadeId = entidadeId;
		this.detalhes = detalhes != null && detalhes.length() > 500 ? detalhes.substring(0, 500) : detalhes;
		this.requestId = requestId;
	}

	public Long getId() { return id; }
	public Long getAtorId() { return atorId; }
	public String getAcao() { return acao; }
	public String getEntidade() { return entidade; }
	public Long getEntidadeId() { return entidadeId; }
	public String getDetalhes() { return detalhes; }
	public String getRequestId() { return requestId; }
	public Instant getCriadoEm() { return criadoEm; }
}
