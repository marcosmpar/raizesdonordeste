package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;

interface UnidadeJpaRepository extends JpaRepository<Unidade, Long> {

	boolean existsByNomeIgnoreCase(String nome);

	boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

	Page<Unidade> findByAtiva(boolean ativa, Pageable pageable);
}
