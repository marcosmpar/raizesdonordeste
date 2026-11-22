package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import br.com.marcosmpar.raizesdonordeste.domain.auditoria.AuditoriaLog;
import br.com.marcosmpar.raizesdonordeste.domain.auditoria.AuditoriaRepository;
import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

@Repository
class AuditoriaRepositoryAdapter implements AuditoriaRepository {

	private final AuditoriaJpaRepository jpa;

	AuditoriaRepositoryAdapter(AuditoriaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public AuditoriaLog salvar(AuditoriaLog log) {
		return jpa.save(log);
	}

	@Override
	public Pagina<AuditoriaLog> listar(String entidade, Long entidadeId, String acao, int page, int limit) {
		Specification<AuditoriaLog> spec = (root, query, cb) -> cb.conjunction();
		if (entidade != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("entidade"), entidade));
		}
		if (entidadeId != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("entidadeId"), entidadeId));
		}
		if (acao != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("acao"), acao));
		}
		return PaginaMapper.pagina(jpa.findAll(spec, PaginaMapper.pageable(page, limit, Sort.by(Sort.Direction.DESC, "id"))));
	}
}
