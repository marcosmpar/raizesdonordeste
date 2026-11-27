package br.com.marcosmpar.raizesdonordeste.api;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.marcosmpar.raizesdonordeste.application.cardapio.CardapioService;
import br.com.marcosmpar.raizesdonordeste.application.estoque.EstoqueService;
import br.com.marcosmpar.raizesdonordeste.application.pagamento.PagamentoService;
import br.com.marcosmpar.raizesdonordeste.application.pedido.PedidoService;
import br.com.marcosmpar.raizesdonordeste.application.pedido.Solicitante;
import br.com.marcosmpar.raizesdonordeste.application.produto.ProdutoService;
import br.com.marcosmpar.raizesdonordeste.application.unidade.UnidadeService;
import br.com.marcosmpar.raizesdonordeste.application.usuario.UsuarioService;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.CanalPedido;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.FormaPagamento;
import br.com.marcosmpar.raizesdonordeste.domain.pedido.Pedido;
import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PagamentoStatusIntegrationTest {

	private static final String SENHA = "Senha@123";
	private static int seq = 0;

	@Autowired MockMvc mvc;
	@Autowired UsuarioService usuarios;
	@Autowired UnidadeService unidades;
	@Autowired ProdutoService produtos;
	@Autowired CardapioService cardapio;
	@Autowired EstoqueService estoque;
	@Autowired PedidoService pedidos;
	@Autowired PagamentoService pagamentos;
	@Autowired JsonMapper json;

	Unidade unidade;
	Produto prato;
	Usuario cliente;
	String tokenCliente, tokenAtendente, tokenCozinha;

	@BeforeEach
	void preparar() throws Exception {
		int n = ++seq;
		unidade = unidades.criar("Pag U" + n, "Recife", "PE", "Rua " + n, true);
		prato = produtos.criar("Prato G" + n, null, "Pratos", new BigDecimal("25.00"));
		cardapio.definir(unidade.getId(), prato.getId(), new BigDecimal("25.00"), true);
		estoque.registrarEntrada(unidade.getId(), prato.getId(), 10, null, null);
		cliente = usuarios.cadastrar("Cli", "cli" + n + "@g.com", SENHA, Perfil.CLIENTE);
		tokenCliente = token("cli" + n + "@g.com");
		usuarios.cadastrar("Atd", "atd" + n + "@g.com", SENHA, Perfil.ATENDENTE);
		tokenAtendente = token("atd" + n + "@g.com");
		usuarios.cadastrar("Coz", "coz" + n + "@g.com", SENHA, Perfil.COZINHA);
		tokenCozinha = token("coz" + n + "@g.com");
	}

	private String token(String email) throws Exception {
		String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + json.readTree(body).get("accessToken").asString();
	}

	/** Cria um pedido de 2 pratos (R$ 50,00) para o cliente. */
	private Pedido novoPedido() {
		return pedidos.criar(cliente.getId(), cliente.getId(), unidade.getId(), CanalPedido.APP, FormaPagamento.MOCK,
				List.of(new PedidoService.ItemComando(prato.getId(), 2)));
	}

	private int saldo() {
		return estoque.saldos(unidade.getId(), 1, 10).conteudo().get(0).getQuantidade();
	}

	private ResultActions pagar(String auth, Long pedidoId, String simulacao, String chave) throws Exception {
		var req = post("/pagamentos").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
				.content("{\"pedidoId\":" + pedidoId + (simulacao == null ? "" : ",\"simulacao\":\"" + simulacao + "\"") + "}");
		if (chave != null) {
			req.header("Idempotency-Key", chave);
		}
		return mvc.perform(req);
	}

	private ResultActions statusPedido(String auth, Long id, String status) throws Exception {
		return mvc.perform(patch("/pedidos/" + id + "/status").header("Authorization", auth)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
	}

	// ---------- pagamento mock ----------

	@Test
	void pagamentoAprovadoRetornaPayloadEMoveOPedidoParaEmPreparo() throws Exception {
		Pedido p = novoPedido();
		pagar(tokenCliente, p.getId(), "APROVADO", null)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("APROVADO"))
				.andExpect(jsonPath("$.statusPedido").value("EM_PREPARO"))
				.andExpect(jsonPath("$.valor").value(50.00))
				.andExpect(jsonPath("$.gateway.codigo").value("APPROVED"))
				.andExpect(jsonPath("$.gateway.transacaoId").isNotEmpty());
		mvc.perform(get("/pedidos/" + p.getId()).header("Authorization", tokenCliente))
				.andExpect(jsonPath("$.status").value("EM_PREPARO"));
	}

	@Test
	void pagamentoRecusadoMantemPedidoAguardandoEPermiteNovaTentativa() throws Exception {
		Pedido p = novoPedido();
		pagar(tokenCliente, p.getId(), "RECUSADO", null)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("RECUSADO"))
				.andExpect(jsonPath("$.statusPedido").value("AGUARDANDO_PAGAMENTO"))
				.andExpect(jsonPath("$.gateway.codigo").value("DECLINED"))
				.andExpect(jsonPath("$.gateway.mensagem").value("Pagamento recusado pelo emissor (simulado)."));
		pagar(tokenCliente, p.getId(), "APROVADO", null)
				.andExpect(jsonPath("$.status").value("APROVADO"))
				.andExpect(jsonPath("$.statusPedido").value("EM_PREPARO"));
		mvc.perform(get("/pedidos/" + p.getId() + "/pagamentos").header("Authorization", tokenCliente))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].status").value("RECUSADO"))
				.andExpect(jsonPath("$[1].status").value("APROVADO"));
	}

	@Test
	void mesmaChaveDeIdempotenciaNaoCobraDeNovo() throws Exception {
		Pedido p = novoPedido();
		String primeiro = pagar(tokenCliente, p.getId(), "APROVADO", "chave-" + seq)
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		pagar(tokenCliente, p.getId(), "APROVADO", "chave-" + seq)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pagamentoId").value(json.readTree(primeiro).get("pagamentoId").asLong()))
				.andExpect(jsonPath("$.status").value("APROVADO"));
		mvc.perform(get("/pedidos/" + p.getId() + "/pagamentos").header("Authorization", tokenCliente))
				.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void gatewayIndisponivelDeixaPendenteERetentativaComMesmaChaveConclui() throws Exception {
		Pedido p = novoPedido();
		String chave = "retry-" + seq;
		pagar(tokenCliente, p.getId(), "INDISPONIVEL", chave)
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.statusPedido").value("AGUARDANDO_PAGAMENTO"))
				.andExpect(jsonPath("$.gateway.codigo").value("GATEWAY_UNAVAILABLE"));
		pagar(tokenCliente, p.getId(), "APROVADO", chave)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("APROVADO"))
				.andExpect(jsonPath("$.statusPedido").value("EM_PREPARO"));
		mvc.perform(get("/pedidos/" + p.getId() + "/pagamentos").header("Authorization", tokenCliente))
				.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void pagarPedidoJaPagoOuCanceladoRetorna409() throws Exception {
		Pedido p = novoPedido();
		pagar(tokenCliente, p.getId(), null, null).andExpect(status().isCreated());
		pagar(tokenCliente, p.getId(), null, null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("PEDIDO_NAO_PAGAVEL"));
		Pedido cancelado = novoPedido();
		mvc.perform(post("/pedidos/" + cancelado.getId() + "/cancelamento").header("Authorization", tokenCliente))
				.andExpect(status().isOk());
		pagar(tokenCliente, cancelado.getId(), null, null).andExpect(status().isConflict());
	}

	@Test
	void validacaoESeguranca() throws Exception {
		Pedido p = novoPedido();
		pagar(tokenCliente, 999999L, null, null).andExpect(status().isNotFound());
		mvc.perform(post("/pagamentos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content("{}")).andExpect(status().isUnprocessableEntity());
		pagar(tokenCliente, p.getId(), "XYZ", null).andExpect(status().isBadRequest());
		mvc.perform(post("/pagamentos").contentType(MediaType.APPLICATION_JSON).content("{\"pedidoId\":1}"))
				.andExpect(status().isUnauthorized());
		pagar(tokenCozinha, p.getId(), null, null).andExpect(status().isForbidden());
		usuarios.cadastrar("Outro", "outro" + seq + "@g.com", SENHA, Perfil.CLIENTE);
		pagar(token("outro" + seq + "@g.com"), p.getId(), null, null).andExpect(status().isNotFound());
	}

	@Test
	void pagamentosSimultaneosDoMesmoPedidoAprovamSoUm() throws Exception {
		Pedido p = novoPedido();
		var solicitante = new Solicitante(cliente.getId(), Perfil.CLIENTE);
		ExecutorService pool = Executors.newFixedThreadPool(6);
		List<Future<Boolean>> futuros = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			futuros.add(pool.submit(() -> {
				try {
					pagamentos.pagar(solicitante, p.getId(), null, null);
					return true;
				}
				catch (RegraNegocioException e) {
					return "PEDIDO_NAO_PAGAVEL".equals(e.getCode()) ? false : null;
				}
			}));
		}
		int aprovados = 0;
		for (Future<Boolean> f : futuros) {
			if (Boolean.TRUE.equals(f.get())) {
				aprovados++;
			}
		}
		pool.shutdown();
		assertEquals(1, aprovados);
	}

	// ---------- status do pedido ----------

	@Test
	void fluxoCompletoCozinhaProntoAtendenteEntrega() throws Exception {
		Pedido p = novoPedido();
		pagar(tokenCliente, p.getId(), null, null).andExpect(status().isCreated());
		statusPedido(tokenCozinha, p.getId(), "PRONTO").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PRONTO"))
				.andExpect(jsonPath("$.itens", hasSize(1)));
		statusPedido(tokenAtendente, p.getId(), "ENTREGUE").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ENTREGUE"));
	}

	@Test
	void transicoesInvalidasRetornam409() throws Exception {
		Pedido p = novoPedido();
		// ainda não pago: não pode ficar pronto
		statusPedido(tokenAtendente, p.getId(), "PRONTO").andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("TRANSICAO_INVALIDA"));
		// status que só existem por outros fluxos
		statusPedido(tokenAtendente, p.getId(), "EM_PREPARO").andExpect(status().isConflict());
		statusPedido(tokenAtendente, p.getId(), "CANCELADO").andExpect(status().isConflict());
		pagar(tokenCliente, p.getId(), null, null);
		// pulando etapa
		statusPedido(tokenAtendente, p.getId(), "ENTREGUE").andExpect(status().isConflict());
	}

	@Test
	void permissoesDeStatus() throws Exception {
		Pedido p = novoPedido();
		pagar(tokenCliente, p.getId(), null, null);
		statusPedido(tokenCliente, p.getId(), "PRONTO").andExpect(status().isForbidden());
		statusPedido(tokenCozinha, p.getId(), "ENTREGUE").andExpect(status().isForbidden());
		statusPedido(tokenCozinha, p.getId(), "PRONTO").andExpect(status().isOk());
		mvc.perform(patch("/pedidos/" + p.getId() + "/status").header("Authorization", tokenAtendente)
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnprocessableEntity());
		statusPedido(tokenAtendente, 999999L, "PRONTO").andExpect(status().isNotFound());
	}

	// ---------- cancelamento ----------

	@Test
	void clienteCancelaPedidoAguardandoEEstoqueVolta() throws Exception {
		Pedido p = novoPedido();
		assertEquals(8, saldo());
		mvc.perform(post("/pedidos/" + p.getId() + "/cancelamento").header("Authorization", tokenCliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADO"));
		assertEquals(10, saldo());
		// cancelar de novo é transição inválida
		mvc.perform(post("/pedidos/" + p.getId() + "/cancelamento").header("Authorization", tokenAtendente))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("TRANSICAO_INVALIDA"));
	}

	@Test
	void clienteNaoCancelaPedidoPagoMasAtendenteCancelaComEstorno() throws Exception {
		Pedido p = novoPedido();
		pagar(tokenCliente, p.getId(), null, null).andExpect(status().isCreated());
		mvc.perform(post("/pedidos/" + p.getId() + "/cancelamento").header("Authorization", tokenCliente))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CANCELAMENTO_NAO_PERMITIDO"));
		mvc.perform(post("/pedidos/" + p.getId() + "/cancelamento").header("Authorization", tokenAtendente))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
		assertEquals(10, saldo());
		mvc.perform(get("/pedidos/" + p.getId() + "/pagamentos").header("Authorization", tokenAtendente))
				.andExpect(jsonPath("$[0].status").value("ESTORNADO"));
	}

	@Test
	void naoCancelaPedidoProntoOuDeOutroCliente() throws Exception {
		Pedido p = novoPedido();
		pagar(tokenCliente, p.getId(), null, null);
		statusPedido(tokenCozinha, p.getId(), "PRONTO");
		mvc.perform(post("/pedidos/" + p.getId() + "/cancelamento").header("Authorization", tokenAtendente))
				.andExpect(status().isConflict());
		Pedido q = novoPedido();
		usuarios.cadastrar("Outro", "outro" + seq + "@g.com", SENHA, Perfil.CLIENTE);
		mvc.perform(post("/pedidos/" + q.getId() + "/cancelamento").header("Authorization", token("outro" + seq + "@g.com")))
				.andExpect(status().isNotFound());
		mvc.perform(post("/pedidos/" + q.getId() + "/cancelamento").header("Authorization", tokenCozinha))
				.andExpect(status().isForbidden());
	}
}
