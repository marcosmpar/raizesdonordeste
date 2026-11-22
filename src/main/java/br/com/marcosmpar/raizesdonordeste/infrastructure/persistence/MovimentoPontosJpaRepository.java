package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.MovimentoPontos;

interface MovimentoPontosJpaRepository extends JpaRepository<MovimentoPontos, Long> {

	Page<MovimentoPontos> findByUsuarioIdOrderByIdDesc(Long usuarioId, Pageable pageable);

	List<MovimentoPontos> findByPedidoId(Long pedidoId);
}
