package br.com.marcosmpar.raizesdonordeste.domain.auditoria;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

public interface AuditoriaRepository {

	AuditoriaLog salvar(AuditoriaLog log);

	/** Filtros opcionais; mais recentes primeiro. */
	Pagina<AuditoriaLog> listar(String entidade, Long entidadeId, String acao, int page, int limit);
}
