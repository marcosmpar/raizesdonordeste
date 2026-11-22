package br.com.marcosmpar.raizesdonordeste.application.fidelidade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcosmpar.raizesdonordeste.application.auditoria.AuditoriaService;
import br.com.marcosmpar.raizesdonordeste.domain.comum.Pagina;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.ContaFidelidade;
import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.FidelidadeRepository;
import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.MovimentoPontos;
import br.com.marcosmpar.raizesdonordeste.domain.fidelidade.TipoMovimentoPontos;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.Pedido;

/**
 * Programa de fidelidade: 1 ponto por R$ 1,00 pago; 1 ponto resgatado = R$ 0,10 de desconto.
 * Acúmulo e resgate exigem consentimento explícito do cliente (LGPD).
 */
@Service
public class FidelidadeService {

	private final FidelidadeRepository repo;
	private final AuditoriaService auditoria;

	public FidelidadeService(FidelidadeRepository repo, AuditoriaService auditoria) {
		this.repo = repo;
		this.auditoria = auditoria;
	}

	@Transactional
	public ContaFidelidade consultar(Long usuarioId) {
		return contaParaAtualizacao(usuarioId);
	}

	@Transactional
	public ContaFidelidade conceder(Long usuarioId) {
		ContaFidelidade conta = contaParaAtualizacao(usuarioId);
		if (!conta.isConsentimento()) {
			conta.conceder();
			repo.salvarConta(conta);
			auditoria.registrar(usuarioId, "FIDELIDADE_CONSENTIMENTO_CONCEDIDO", "CONTA_FIDELIDADE", usuarioId, null);
		}
		return conta;
	}

	@Transactional
	public ContaFidelidade revogar(Long usuarioId) {
		ContaFidelidade conta = contaParaAtualizacao(usuarioId);
		if (conta.isConsentimento()) {
			conta.revogar();
			repo.salvarConta(conta);
			auditoria.registrar(usuarioId, "FIDELIDADE_CONSENTIMENTO_REVOGADO", "CONTA_FIDELIDADE", usuarioId, null);
		}
		return conta;
	}

	@Transactional(readOnly = true)
	public Pagina<MovimentoPontos> historico(Long usuarioId, int page, int limit) {
		return repo.historico(usuarioId, page, limit);
	}

	/** Valida o resgate (sem debitar) e devolve o desconto em reais. */
	@Transactional(readOnly = true)
	public BigDecimal calcularDescontoResgate(Long clienteId, int pontos, BigDecimal totalAtual) {
		if (clienteId == null) {
			throw new RegraNegocioException("FIDELIDADE_REQUER_CLIENTE",
					"O resgate de pontos exige um cliente identificado no pedido.");
		}
		ContaFidelidade conta = repo.buscarConta(clienteId).orElse(null);
		if (conta == null || !conta.isConsentimento()) {
			throw new RegraNegocioException("FIDELIDADE_SEM_CONSENTIMENTO",
					"O cliente não consentiu com o programa de fidelidade.");
		}
		if (pontos > conta.getSaldoPontos()) {
			throw ContaFidelidade.pontosInsuficientes(conta.getSaldoPontos());
		}
		BigDecimal desconto = ContaFidelidade.VALOR_PONTO.multiply(BigDecimal.valueOf(pontos));
		if (desconto.compareTo(totalAtual) > 0) {
			int maximo = totalAtual.divide(ContaFidelidade.VALOR_PONTO, 0, RoundingMode.DOWN).intValue();
			throw new RegraNegocioException("RESGATE_EXCEDE_TOTAL", "O desconto excede o valor do pedido.",
					List.of(new RegraNegocioException.Detalhe("pontosResgatar", "Máximo para este pedido: " + maximo)));
		}
		return desconto;
	}

	/** Debita os pontos do resgate (com bloqueio da conta) e registra o movimento. */
	@Transactional
	public void debitarResgate(Long clienteId, int pontos, Long pedidoId) {
		ContaFidelidade conta = contaParaAtualizacao(clienteId);
		conta.debitar(pontos);
		repo.salvarConta(conta);
		repo.registrar(new MovimentoPontos(clienteId, TipoMovimentoPontos.RESGATE, -pontos, pedidoId,
				"Resgate no pedido #" + pedidoId));
		auditoria.registrar(clienteId, "FIDELIDADE_RESGATE", "PEDIDO", pedidoId, "pontos=" + pontos);
	}

	/** Chamado quando o pagamento é aprovado: acumula pontos se o cliente consentiu. */
	@Transactional
	public void acumular(Pedido pedido) {
		if (pedido.getCliente() == null) {
			return;
		}
		Long clienteId = pedido.getCliente().getId();
		ContaFidelidade conta = contaParaAtualizacao(clienteId);
		if (!conta.isConsentimento()) {
			return;
		}
		int pontos = pedido.getTotal().setScale(0, RoundingMode.DOWN).intValue() * ContaFidelidade.PONTOS_POR_REAL;
		if (pontos > 0) {
			conta.creditar(pontos);
			repo.salvarConta(conta);
			repo.registrar(new MovimentoPontos(clienteId, TipoMovimentoPontos.ACUMULO, pontos, pedido.getId(),
					"Pontos do pedido #" + pedido.getId()));
		}
	}

	/** Cancelamento: devolve pontos resgatados e retira os acumulados (sem deixar o saldo negativo). */
	@Transactional
	public void estornarDoPedido(Pedido pedido) {
		if (pedido.getCliente() == null) {
			return;
		}
		Long clienteId = pedido.getCliente().getId();
		List<MovimentoPontos> movimentos = repo.movimentosDoPedido(pedido.getId());
		int acumulados = movimentos.stream().filter(m -> m.getTipo() == TipoMovimentoPontos.ACUMULO)
				.mapToInt(MovimentoPontos::getPontos).sum();
		int resgatados = -movimentos.stream().filter(m -> m.getTipo() == TipoMovimentoPontos.RESGATE)
				.mapToInt(MovimentoPontos::getPontos).sum();
		if (acumulados == 0 && resgatados == 0) {
			return;
		}
		ContaFidelidade conta = contaParaAtualizacao(clienteId);
		if (resgatados > 0) {
			conta.creditar(resgatados);
			repo.registrar(new MovimentoPontos(clienteId, TipoMovimentoPontos.ESTORNO, resgatados, pedido.getId(),
					"Devolução de pontos do pedido cancelado #" + pedido.getId()));
		}
		if (acumulados > 0) {
			int retirar = Math.min(acumulados, conta.getSaldoPontos());
			if (retirar > 0) {
				conta.debitar(retirar);
				repo.registrar(new MovimentoPontos(clienteId, TipoMovimentoPontos.ESTORNO, -retirar, pedido.getId(),
						"Estorno de pontos do pedido cancelado #" + pedido.getId()));
			}
		}
		repo.salvarConta(conta);
	}

	private ContaFidelidade contaParaAtualizacao(Long usuarioId) {
		return repo.buscarContaParaAtualizacao(usuarioId)
				.orElseGet(() -> repo.salvarConta(new ContaFidelidade(usuarioId)));
	}
}
