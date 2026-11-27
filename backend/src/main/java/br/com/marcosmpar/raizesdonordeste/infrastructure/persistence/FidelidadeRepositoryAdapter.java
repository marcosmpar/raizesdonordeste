package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.ContaFidelidade;
import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.FidelidadeRepository;
import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.MovimentoPontos;

@Repository
class FidelidadeRepositoryAdapter implements FidelidadeRepository {

	private final ContaFidelidadeJpaRepository contas;
	private final MovimentoPontosJpaRepository movimentos;

	FidelidadeRepositoryAdapter(ContaFidelidadeJpaRepository contas, MovimentoPontosJpaRepository movimentos) {
		this.contas = contas;
		this.movimentos = movimentos;
	}

	@Override
	public Optional<ContaFidelidade> buscarConta(Long usuarioId) {
		return contas.findById(usuarioId);
	}

	@Override
	public Optional<ContaFidelidade> buscarContaParaAtualizacao(Long usuarioId) {
		return contas.buscarComLock(usuarioId);
	}

	@Override
	public ContaFidelidade salvarConta(ContaFidelidade conta) {
		return contas.saveAndFlush(conta);
	}

	@Override
	public MovimentoPontos registrar(MovimentoPontos m) {
		return movimentos.save(m);
	}

	@Override
	public Pagina<MovimentoPontos> historico(Long usuarioId, int page, int limit) {
		return PaginaMapper.pagina(movimentos.findByUsuarioIdOrderByIdDesc(usuarioId, PaginaMapper.pageable(page, limit)));
	}

	@Override
	public List<MovimentoPontos> movimentosDoPedido(Long pedidoId) {
		return movimentos.findByPedidoId(pedidoId);
	}
}
