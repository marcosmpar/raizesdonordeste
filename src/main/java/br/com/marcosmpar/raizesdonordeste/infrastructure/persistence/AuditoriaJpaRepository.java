package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.marcosmpar.raizesdonordeste.domain.auditoria.AuditoriaLog;

interface AuditoriaJpaRepository extends JpaRepository<AuditoriaLog, Long>, JpaSpecificationExecutor<AuditoriaLog> {
}
