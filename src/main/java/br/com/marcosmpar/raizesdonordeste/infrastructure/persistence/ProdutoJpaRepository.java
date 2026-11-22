package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;

interface ProdutoJpaRepository extends JpaRepository<Produto, Long> {

	boolean existsByNomeIgnoreCase(String nome);

	boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

	Page<Produto> findByAtivoTrue(Pageable pageable);
}
