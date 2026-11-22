package br.com.marcosmpar.raizesdonordeste.api.auditoria;

import java.time.Instant;

import br.com.marcosmpar.raizesdonordeste.domain.auditoria.AuditoriaLog;

public record AuditoriaResponse(Long id, Long atorId, String acao, String entidade, Long entidadeId,
		String detalhes, String requestId, Instant createdAt) {

	public static AuditoriaResponse de(AuditoriaLog l) {
		return new AuditoriaResponse(l.getId(), l.getAtorId(), l.getAcao(), l.getEntidade(), l.getEntidadeId(),
				l.getDetalhes(), l.getRequestId(), l.getCriadoEm());
	}
}
