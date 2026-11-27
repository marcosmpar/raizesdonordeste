package br.com.marcosmpar.raizesdonordeste.application.auditoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcosmpar.raizesdonordeste.domain.auditoria.AuditoriaLog;
import br.com.marcosmpar.raizesdonordeste.domain.auditoria.AuditoriaRepository;
import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

/**
 * Registra ações sensíveis na mesma transação da ação (se a ação falhar, o registro também é desfeito).
 * Nunca incluir dados pessoais (e-mail, nome) nem segredos em {@code detalhes}.
 */
@Service
public class AuditoriaService {

	private static final Logger log = LoggerFactory.getLogger("AUDITORIA");

	private final AuditoriaRepository repo;

	public AuditoriaService(AuditoriaRepository repo) {
		this.repo = repo;
	}

	@Transactional
	public void registrar(Long atorId, String acao, String entidade, Long entidadeId, String detalhes) {
		repo.salvar(new AuditoriaLog(atorId, acao, entidade, entidadeId, detalhes, MDC.get("requestId")));
		log.info("ator={} acao={} entidade={}#{} {}", atorId, acao, entidade, entidadeId, detalhes);
	}

	@Transactional(readOnly = true)
	public Pagina<AuditoriaLog> listar(String entidade, Long entidadeId, String acao, int page, int limit) {
		return repo.listar(entidade, entidadeId, acao, page, limit);
	}
}
