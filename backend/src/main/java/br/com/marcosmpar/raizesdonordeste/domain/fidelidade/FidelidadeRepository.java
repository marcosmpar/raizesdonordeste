package br.com.marcosmpar.raizesdonordeste.domain.fidelidade;

import java.util.List;
import java.util.Optional;

import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;

public interface FidelidadeRepository {

	Optional<ContaFidelidade> buscarConta(Long usuarioId);

	/** Com bloqueio de escrita: evita corrida entre resgates/acúmulos do mesmo cliente. */
	Optional<ContaFidelidade> buscarContaParaAtualizacao(Long usuarioId);

	ContaFidelidade salvarConta(ContaFidelidade conta);

	MovimentoPontos registrar(MovimentoPontos movimento);

	/** Mais recentes primeiro. */
	Pagina<MovimentoPontos> historico(Long usuarioId, int page, int limit);

	List<MovimentoPontos> movimentosDoPedido(Long pedidoId);
}
