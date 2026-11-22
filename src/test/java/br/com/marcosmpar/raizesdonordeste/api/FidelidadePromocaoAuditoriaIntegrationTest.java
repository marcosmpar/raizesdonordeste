package br.com.marcosmpar.raizesdonordeste.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
import br.com.marcosmpar.raizesdonordeste.application.produto.ProdutoService;
import br.com.marcosmpar.raizesdonordeste.application.unidade.UnidadeService;
import br.com.marcosmpar.raizesdonordeste.application.usuario.UsuarioService;
import br.com.marcosmpar.raizesdonordeste.domain.produto.Produto;
import br.com.marcosmpar.raizesdonordeste.domain.unidade.Unidade;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FidelidadePromocaoAuditoriaIntegrationTest {

	private static final String SENHA = "Senha@123";
	private static int seq = 0;

	@Autowired MockMvc mvc;
	@Autowired UsuarioService usuarios;
	@Autowired UnidadeService unidades;
	@Autowired ProdutoService produtos;
	@Autowired CardapioService cardapio;
	@Autowired EstoqueService estoque;
	@Autowired JsonMapper json;

	int n;
	Unidade unidade;
	Produto prato; // R$ 25,00 na unidade
	Usuario cliente;
	String tokenCliente, tokenAtendente, tokenGerente;

	@BeforeEach
	void preparar() throws Exception {
		n = ++seq;
		unidade = unidades.criar("FPA U" + n, "Recife", "PE", "Rua " + n, true);
		prato = produtos.criar("Prato F" + n, null, "Pratos", new BigDecimal("25.00"));
		cardapio.definir(unidade.getId(), prato.getId(), new BigDecimal("25.00"), true);
		estoque.registrarEntrada(unidade.getId(), prato.getId(), 1000, null, null);
		cliente = usuarios.cadastrar("Cli", "cli" + n + "@f.com", SENHA, Perfil.CLIENTE);
		tokenCliente = token("cli" + n + "@f.com");
		usuarios.cadastrar("Atd", "atd" + n + "@f.com", SENHA, Perfil.ATENDENTE);
		tokenAtendente = token("atd" + n + "@f.com");
		usuarios.cadastrar("Ger", "ger" + n + "@f.com", SENHA, Perfil.GERENTE);
		tokenGerente = token("ger" + n + "@f.com");
	}

	private String token(String email) throws Exception {
		String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + json.readTree(body).get("accessToken").asString();
	}

	private ResultActions criarPedido(int pratos, String promo, Integer pontos) throws Exception {
		String extra = (promo == null ? "" : ",\"codigoPromocao\":\"" + promo + "\"")
				+ (pontos == null ? "" : ",\"pontosResgatar\":" + pontos);
		return mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content("{\"canalPedido\":\"APP\",\"unidadeId\":" + unidade.getId() + ",\"itens\":[{\"produtoId\":"
						+ prato.getId() + ",\"quantidade\":" + pratos + "}],\"formaPagamento\":\"MOCK\"" + extra + "}"));
	}

	private long pedidoCriado(int pratos, String promo, Integer pontos) throws Exception {
		String corpo = criarPedido(pratos, promo, pontos).andExpect(status().isCreated()).andReturn().getResponse()
				.getContentAsString();
		return json.readTree(corpo).get("pedidoId").asLong();
	}

	private void pagar(long pedidoId) throws Exception {
		mvc.perform(post("/pagamentos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content("{\"pedidoId\":" + pedidoId + "}")).andExpect(status().isCreated());
	}

	private void consentir() throws Exception {
		mvc.perform(post("/fidelidade/consentimento").header("Authorization", tokenCliente)).andExpect(status().isOk());
	}

	/** Dá pontos ao cliente: paga um pedido de (pontos/25) pratos. 1 ponto por R$ 1,00. */
	private void ganharPontos(int pontos) throws Exception {
		pagar(pedidoCriado(pontos / 25, null, null));
	}

	private ResultActions conta() throws Exception {
		return mvc.perform(get("/fidelidade").header("Authorization", tokenCliente));
	}

	private String promocao(String codigo, String tipo, String valor, String minimo, Long unidadeId, Instant inicio,
			Instant fim) throws Exception {
		String corpo = "{\"codigo\":\"" + codigo + "\",\"descricao\":\"Campanha " + codigo + "\",\"tipo\":\"" + tipo
				+ "\",\"valor\":" + valor + (minimo == null ? "" : ",\"valorMinimoPedido\":" + minimo)
				+ (unidadeId == null ? "" : ",\"unidadeId\":" + unidadeId) + ",\"inicio\":\"" + inicio
				+ "\",\"fim\":\"" + fim + "\"}";
		mvc.perform(post("/promocoes").header("Authorization", tokenGerente).contentType(MediaType.APPLICATION_JSON)
				.content(corpo)).andExpect(status().isCreated());
		return codigo;
	}

	private String vigente(String sufixo, String tipo, String valor, String minimo, Long unidadeId) throws Exception {
		Instant agora = Instant.now();
		return promocao("P" + n + sufixo, tipo, valor, minimo, unidadeId, agora.minus(1, ChronoUnit.DAYS),
				agora.plus(30, ChronoUnit.DAYS));
	}

	// ---------- fidelidade ----------

	@Test
	void pontosSoAcumulamComConsentimentoERevogacaoPreservaSaldo() throws Exception {
		conta().andExpect(status().isOk()).andExpect(jsonPath("$.consentimento").value(false))
				.andExpect(jsonPath("$.saldoPontos").value(0))
				.andExpect(jsonPath("$.regras.pontosPorReal").value(1));

		ganharPontos(50); // sem consentimento: não acumula
		conta().andExpect(jsonPath("$.saldoPontos").value(0));

		consentir();
		ganharPontos(50);
		conta().andExpect(jsonPath("$.consentimento").value(true)).andExpect(jsonPath("$.saldoPontos").value(50));
		mvc.perform(get("/fidelidade/historico").header("Authorization", tokenCliente))
				.andExpect(jsonPath("$.content[0].tipo").value("ACUMULO"))
				.andExpect(jsonPath("$.content[0].pontos").value(50));

		mvc.perform(delete("/fidelidade/consentimento").header("Authorization", tokenCliente))
				.andExpect(status().isOk()).andExpect(jsonPath("$.consentimento").value(false))
				.andExpect(jsonPath("$.saldoPontos").value(50));
		ganharPontos(50); // revogado: não acumula mais
		conta().andExpect(jsonPath("$.saldoPontos").value(50));
	}

	@Test
	void resgateGeraDescontoEDebitaECancelamentoDevolveOsPontos() throws Exception {
		consentir();
		ganharPontos(50);
		long id = json.readTree(criarPedido(2, null, 30).andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotal").value(50.00))
				.andExpect(jsonPath("$.descontoFidelidade").value(3.00))
				.andExpect(jsonPath("$.pontosResgatados").value(30))
				.andExpect(jsonPath("$.total").value(47.00))
				.andReturn().getResponse().getContentAsString()).get("pedidoId").asLong();
		conta().andExpect(jsonPath("$.saldoPontos").value(20));

		mvc.perform(post("/pedidos/" + id + "/cancelamento").header("Authorization", tokenCliente))
				.andExpect(status().isOk());
		conta().andExpect(jsonPath("$.saldoPontos").value(50));
		mvc.perform(get("/fidelidade/historico").header("Authorization", tokenCliente))
				.andExpect(jsonPath("$.content[0].tipo").value("ESTORNO"))
				.andExpect(jsonPath("$.content[0].pontos").value(30));
	}

	@Test
	void cancelarPedidoPagoRetiraOsPontosAcumulados() throws Exception {
		consentir();
		long id = pedidoCriado(2, null, null);
		pagar(id);
		conta().andExpect(jsonPath("$.saldoPontos").value(50));
		mvc.perform(post("/pedidos/" + id + "/cancelamento").header("Authorization", tokenAtendente))
				.andExpect(status().isOk());
		conta().andExpect(jsonPath("$.saldoPontos").value(0));
		mvc.perform(get("/fidelidade/historico").header("Authorization", tokenCliente))
				.andExpect(jsonPath("$.content[0].tipo").value("ESTORNO"))
				.andExpect(jsonPath("$.content[0].pontos").value(-50));
	}

	@Test
	void resgateInvalidoRetorna409() throws Exception {
		criarPedido(1, null, 10).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("FIDELIDADE_SEM_CONSENTIMENTO"));
		consentir();
		criarPedido(1, null, 10).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("PONTOS_INSUFICIENTES"))
				.andExpect(jsonPath("$.details[0].issue").value("Disponível: 0"));
		ganharPontos(500);
		// 300 pontos = R$ 30,00 > pedido de R$ 25,00
		criarPedido(1, null, 300).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("RESGATE_EXCEDE_TOTAL"))
				.andExpect(jsonPath("$.details[0].issue").value("Máximo para este pedido: 250"));
		criarPedido(1, null, -5).andExpect(status().isUnprocessableEntity());
		conta().andExpect(jsonPath("$.saldoPontos").value(500)); // nada foi debitado
	}

	@Test
	void fidelidadeEExclusivaDoCliente() throws Exception {
		mvc.perform(get("/fidelidade")).andExpect(status().isUnauthorized());
		mvc.perform(get("/fidelidade").header("Authorization", tokenAtendente)).andExpect(status().isForbidden());
		mvc.perform(post("/fidelidade/consentimento").header("Authorization", tokenGerente))
				.andExpect(status().isForbidden());
	}

	// ---------- promoções ----------

	@Test
	void promocaoPercentualReduzOTotal() throws Exception {
		String codigo = vigente("A", "PERCENTUAL", "10", "30.00", null);
		criarPedido(2, codigo.toLowerCase(), null).andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotal").value(50.00))
				.andExpect(jsonPath("$.descontoPromocao").value(5.00))
				.andExpect(jsonPath("$.codigoPromocao").value(codigo))
				.andExpect(jsonPath("$.total").value(45.00));
	}

	@Test
	void promocaoDeValorFixoNuncaPassaDoSubtotal() throws Exception {
		String codigo = vigente("B", "VALOR_FIXO", "100", null, unidade.getId());
		criarPedido(1, codigo, null).andExpect(status().isCreated())
				.andExpect(jsonPath("$.descontoPromocao").value(25.00))
				.andExpect(jsonPath("$.total").value(0.00));
	}

	@Test
	void promocaoSeAplicaAntesDoResgateDePontos() throws Exception {
		consentir();
		ganharPontos(50);
		String codigo = vigente("C", "PERCENTUAL", "20", null, null);
		criarPedido(2, codigo, 10).andExpect(status().isCreated())
				.andExpect(jsonPath("$.descontoPromocao").value(10.00))
				.andExpect(jsonPath("$.descontoFidelidade").value(1.00))
				.andExpect(jsonPath("$.total").value(39.00));
	}

	@Test
	void promocaoNaoAplicavelRetorna409OuNaoExistenteRetorna404() throws Exception {
		Instant agora = Instant.now();
		Unidade outra = unidades.criar("FPA Outra" + n, "Olinda", "PE", "Rua O", true);
		String expirada = promocao("P" + n + "EXP", "VALOR_FIXO", "5", null, null, agora.minus(10, ChronoUnit.DAYS),
				agora.minus(5, ChronoUnit.DAYS));
		String futura = promocao("P" + n + "FUT", "VALOR_FIXO", "5", null, null, agora.plus(5, ChronoUnit.DAYS),
				agora.plus(10, ChronoUnit.DAYS));
		String minimo = vigente("MIN", "VALOR_FIXO", "5", "100.00", null);
		String outraUnidade = vigente("UNI", "VALOR_FIXO", "5", null, outra.getId());
		String encerrada = vigente("END", "VALOR_FIXO", "5", null, null);

		for (String codigo : List.of(expirada, futura, minimo, outraUnidade)) {
			criarPedido(1, codigo, null).andExpect(status().isConflict())
					.andExpect(jsonPath("$.error").value("PROMOCAO_INVALIDA"))
					.andExpect(jsonPath("$.details[0].field").value("codigoPromocao"));
		}
		Long id = json.readTree(mvc.perform(get("/promocoes?limit=100")).andReturn().getResponse().getContentAsString())
				.get("content").valueStream().filter(p -> p.get("codigo").asString().equals(encerrada)).findFirst()
				.orElseThrow().get("id").asLong();
		mvc.perform(delete("/promocoes/" + id).header("Authorization", tokenGerente)).andExpect(status().isNoContent());
		criarPedido(1, encerrada, null).andExpect(status().isConflict());

		criarPedido(1, "NAOEXISTE", null).andExpect(status().isNotFound());
	}

	@Test
	void cadastroDePromocaoValidaEProtege() throws Exception {
		Instant agora = Instant.now();
		String base = "{\"codigo\":\"XPR%s\",\"descricao\":\"d\",\"tipo\":\"%s\",\"valor\":%s,\"inicio\":\"%s\",\"fim\":\"%s\"}";
		mvc.perform(post("/promocoes").header("Authorization", tokenGerente).contentType(MediaType.APPLICATION_JSON)
				.content(base.formatted(n, "PERCENTUAL", "150", agora, agora.plus(1, ChronoUnit.DAYS))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.details[0].field").value("percentualValido"));
		mvc.perform(post("/promocoes").header("Authorization", tokenGerente).contentType(MediaType.APPLICATION_JSON)
				.content(base.formatted(n, "VALOR_FIXO", "5", agora, agora.minus(1, ChronoUnit.DAYS))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.details[0].field").value("periodoValido"));
		mvc.perform(post("/promocoes").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(base.formatted(n, "VALOR_FIXO", "5", agora, agora.plus(1, ChronoUnit.DAYS))))
				.andExpect(status().isForbidden());
		String codigo = vigente("DUP", "VALOR_FIXO", "5", null, null);
		mvc.perform(post("/promocoes").header("Authorization", tokenGerente).contentType(MediaType.APPLICATION_JSON)
				.content("{\"codigo\":\"" + codigo + "\",\"descricao\":\"d\",\"tipo\":\"VALOR_FIXO\",\"valor\":5,\"inicio\":\""
						+ agora.minus(1, ChronoUnit.DAYS) + "\",\"fim\":\"" + agora.plus(1, ChronoUnit.DAYS) + "\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("PROMOCAO_JA_CADASTRADA"));
		// listagem pública mostra só vigentes
		mvc.perform(get("/promocoes?limit=100")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].codigo", hasItem(codigo)));
	}

	// ---------- auditoria ----------

	@Test
	void acoesSensiveisGeramRegistroDeAuditoria() throws Exception {
		long id = pedidoCriado(2, null, null);
		pagar(id);
		mvc.perform(patch("/pedidos/" + id + "/status").header("Authorization", tokenAtendente)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PRONTO\"}")).andExpect(status().isOk());
		mvc.perform(post("/pedidos/" + id + "/cancelamento").header("Authorization", tokenGerente))
				.andExpect(status().isConflict()); // PRONTO não cancela: nada a registrar
		long id2 = pedidoCriado(1, null, null);
		mvc.perform(post("/pedidos/" + id2 + "/cancelamento").header("Authorization", tokenCliente))
				.andExpect(status().isOk());

		mvc.perform(get("/auditoria?entidade=PEDIDO&entidadeId=" + id).header("Authorization", tokenGerente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].acao", hasItem("PEDIDO_CRIADO")))
				.andExpect(jsonPath("$.content[*].acao", hasItem("PEDIDO_STATUS_ALTERADO")))
				.andExpect(jsonPath("$.content[?(@.acao=='PEDIDO_CRIADO')].atorId", hasItem((int) (long) cliente.getId())))
				.andExpect(jsonPath("$.content[?(@.acao=='PEDIDO_CRIADO')].detalhes", hasItem(containsString("canal=APP"))))
				.andExpect(jsonPath("$.content[?(@.acao=='PEDIDO_CRIADO')].requestId", not(hasItem(nullValue()))));
		mvc.perform(get("/auditoria?entidade=PEDIDO&entidadeId=" + id2 + "&acao=PEDIDO_CANCELADO")
				.header("Authorization", tokenGerente))
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].detalhes").value("status anterior=AGUARDANDO_PAGAMENTO"));
		mvc.perform(get("/auditoria?entidade=PAGAMENTO&acao=PAGAMENTO_PROCESSADO&limit=100")
				.header("Authorization", tokenGerente))
				.andExpect(jsonPath("$.content[*].detalhes", hasItem(containsString("pedido=" + id + " status=APROVADO"))));
	}

	@Test
	void auditoriaNaoGuardaDadosPessoaisEEhRestrita() throws Exception {
		usuarios.cadastrar("Novo", "novo" + n + "@f.com", SENHA, Perfil.COZINHA, cliente.getId());
		mvc.perform(get("/auditoria?acao=USUARIO_CRIADO&limit=100").header("Authorization", tokenGerente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].detalhes", hasItem("perfil=COZINHA")))
				.andExpect(jsonPath("$.content[*].detalhes", not(hasItem(containsString("@")))));
		mvc.perform(get("/auditoria")).andExpect(status().isUnauthorized());
		mvc.perform(get("/auditoria").header("Authorization", tokenCliente)).andExpect(status().isForbidden());
		mvc.perform(get("/auditoria").header("Authorization", tokenAtendente)).andExpect(status().isForbidden());
	}
}
