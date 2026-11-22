package br.com.marcosmpar.raizesdonordeste.application.pagamento;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcosmpar.raizesdonordeste.application.auditoria.AuditoriaService;
import br.com.marcosmpar.raizesdonordeste.application.fidelidade.FidelidadeService;
import br.com.marcosmpar.raizesdonordeste.application.pedido.Solicitante;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.pagamento.Pagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pagamento.PagamentoRepository;
import br.com.marcosmpar.raizesdonordeste.domain.pagamento.SimulacaoPagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pagamento.StatusPagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.Pedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.PedidoRepository;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.StatusPedido;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;

@Service
public class PagamentoService {

	private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

	/** reaproveitado = resposta idempotente de uma tentativa já concluída (nenhuma nova cobrança). */
	public record Resultado(Pagamento pagamento, boolean reaproveitado) {
	}

	private final PagamentoRepository pagamentos;
	private final PedidoRepository pedidos;
	private final GatewayPagamento gateway;
	private final FidelidadeService fidelidade;
	private final AuditoriaService auditoria;

	public PagamentoService(PagamentoRepository pagamentos, PedidoRepository pedidos, GatewayPagamento gateway,
			FidelidadeService fidelidade, AuditoriaService auditoria) {
		this.pagamentos = pagamentos;
		this.pedidos = pedidos;
		this.gateway = gateway;
		this.fidelidade = fidelidade;
		this.auditoria = auditoria;
	}

	/**
	 * Solicita o pagamento do pedido ao gateway (mock) e registra o resultado.
	 * Aprovado → pedido EM_PREPARO. Recusado → pedido segue AGUARDANDO_PAGAMENTO (pode tentar de novo).
	 * Gateway fora do ar → tentativa PENDENTE; repetir com a mesma chave de idempotência.
	 * O pedido fica bloqueado durante a chamada (o gateway é um mock instantâneo; um gateway real
	 * exigiria separar a chamada externa da transação).
	 */
	@Transactional
	public Resultado pagar(Solicitante solicitante, Long pedidoId, String chaveIdempotencia,
			SimulacaoPagamento simulacao) {
		Pedido pedido = pedidos.buscarParaAtualizacao(pedidoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", pedidoId));
		exigirAcesso(pedido, solicitante);

		Pagamento pagamento = null;
		if (chaveIdempotencia != null) {
			pagamento = pagamentos.buscarPorChave(pedidoId, chaveIdempotencia).orElse(null);
			if (pagamento != null && pagamento.getStatus() != StatusPagamento.PENDENTE) {
				return new Resultado(pagamento, true); // mesma chave: devolve o resultado original
			}
		}
		if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
			throw new RegraNegocioException("PEDIDO_NAO_PAGAVEL",
					"O pedido não está aguardando pagamento.",
					List.of(new RegraNegocioException.Detalhe("status", "Status atual: " + pedido.getStatus())));
		}
		if (pagamento == null) {
			pagamento = new Pagamento(pedido, chaveIdempotencia);
		}

		try {
			var resposta = gateway.processar(new GatewayPagamento.Solicitacao(pedido.getId(), pedido.getTotal(),
					pedido.getFormaPagamento(), simulacao));
			if (resposta.aprovado()) {
				pagamento.aprovar(resposta.transacaoId(), resposta.codigo(), resposta.mensagem());
				pedido.confirmarPagamento();
				fidelidade.acumular(pedido);
			}
			else {
				pagamento.recusar(resposta.transacaoId(), resposta.codigo(), resposta.mensagem());
			}
		}
		catch (GatewayIndisponivelException e) {
			log.warn("Gateway indisponível para o pedido {}: {}", pedidoId, e.getMessage());
			pagamento.manterPendente("GATEWAY_UNAVAILABLE",
					"Gateway indisponível. Tente novamente com a mesma Idempotency-Key.");
		}
		pedidos.salvar(pedido);
		Pagamento salvo = pagamentos.salvar(pagamento);
		auditoria.registrar(solicitante.id(), "PAGAMENTO_PROCESSADO", "PAGAMENTO", salvo.getId(),
				"pedido=" + pedidoId + " status=" + salvo.getStatus() + " valor=" + salvo.getValor());
		return new Resultado(salvo, false);
	}

	@Transactional(readOnly = true)
	public Pagamento buscar(Long id, Solicitante solicitante) {
		Pagamento p = pagamentos.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento", id));
		exigirAcesso(p.getPedido(), solicitante);
		return p;
	}

	@Transactional(readOnly = true)
	public List<Pagamento> listarDoPedido(Long pedidoId, Solicitante solicitante) {
		Pedido pedido = pedidos.buscarPorId(pedidoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", pedidoId));
		exigirAcesso(pedido, solicitante);
		return pagamentos.listarPorPedido(pedidoId);
	}

	/** Estorna (no gateway mock) os pagamentos aprovados do pedido cancelado. */
	@Transactional
	public void estornarDoPedido(Pedido pedido) {
		for (Pagamento p : pagamentos.listarPorPedido(pedido.getId())) {
			if (p.getStatus() == StatusPagamento.APROVADO) {
				var resposta = gateway.estornar(p.getTransacaoGateway(), p.getValor());
				p.estornar(resposta.mensagem());
				pagamentos.salvar(p);
			}
		}
	}

	/** CLIENTE só acessa pedidos próprios (404 para não revelar a existência de pedidos alheios). */
	private static void exigirAcesso(Pedido pedido, Solicitante s) {
		if (s.perfil() == Perfil.CLIENTE
				&& (pedido.getCliente() == null || !pedido.getCliente().getId().equals(s.id()))) {
			throw new RecursoNaoEncontradoException("Pedido", pedido.getId());
		}
	}
}
