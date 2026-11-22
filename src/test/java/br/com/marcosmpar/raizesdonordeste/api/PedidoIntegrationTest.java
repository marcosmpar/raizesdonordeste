package br.com.marcosmpar.raizesdonordeste.api;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
class PedidoIntegrationTest {

	private static final String SENHA = "Senha@123";
	private static int seq = 0;

	@Autowired MockMvc mvc;
	@Autowired UsuarioService usuarios;
	@Autowired UnidadeService unidades;
	@Autowired ProdutoService produtos;
	@Autowired CardapioService cardapio;
	@Autowired EstoqueService estoque;
	@Autowired JsonMapper json;

	Unidade unidade;
	Produto baiao;   // 20.00 na unidade, estoque 5
	Produto suco;    // 5.50 na unidade, estoque 5
	Produto fora;    // existe, mas não está no cardápio da unidade
	Produto oculto;  // no cardápio, indisponível
	Usuario cliente;
	String tokenCliente;

	@BeforeEach
	void preparar() throws Exception {
		int n = ++seq;
		unidade = unidades.criar("Pedido U" + n, "Recife", "PE", "Rua " + n, true);
		baiao = produtos.criar("Baião P" + n, null, "Pratos", new BigDecimal("30.00"));
		suco = produtos.criar("Suco P" + n, null, "Bebidas", new BigDecimal("9.00"));
		fora = produtos.criar("Fora P" + n, null, "Pratos", new BigDecimal("10.00"));
		oculto = produtos.criar("Oculto P" + n, null, "Pratos", new BigDecimal("10.00"));
		cardapio.definir(unidade.getId(), baiao.getId(), new BigDecimal("20.00"), true);
		cardapio.definir(unidade.getId(), suco.getId(), new BigDecimal("5.50"), true);
		cardapio.definir(unidade.getId(), oculto.getId(), new BigDecimal("10.00"), false);
		estoque.registrarEntrada(unidade.getId(), baiao.getId(), 5, null, null);
		estoque.registrarEntrada(unidade.getId(), suco.getId(), 5, null, null);
		estoque.registrarEntrada(unidade.getId(), oculto.getId(), 5, null, null);
		cliente = usuarios.cadastrar("Cliente", "cli" + n + "@p.com", SENHA, Perfil.CLIENTE);
		tokenCliente = token("cli" + n + "@p.com");
	}

	private String token(String email) throws Exception {
		String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + json.readTree(body).get("accessToken").asString();
	}

	private String tokenPerfil(String prefixo, Perfil perfil) throws Exception {
		String email = prefixo + seq + "@p.com";
		usuarios.cadastrar(prefixo, email, SENHA, perfil);
		return token(email);
	}

	private String pedido(String canal, Long clienteId, Long unidadeId, String itens) {
		return "{" + (canal == null ? "" : "\"canalPedido\":\"" + canal + "\",")
				+ (clienteId == null ? "" : "\"clienteId\":" + clienteId + ",")
				+ "\"unidadeId\":" + unidadeId + ",\"itens\":" + itens + ",\"formaPagamento\":\"MOCK\"}";
	}

	private String item(Long produtoId, int qtd) {
		return "{\"produtoId\":" + produtoId + ",\"quantidade\":" + qtd + "}";
	}

	private int saldo(Produto p) {
		return estoque.saldos(unidade.getId(), 1, 100).conteudo().stream()
				.filter(e -> e.getProduto().getId().equals(p.getId())).findFirst().orElseThrow().getQuantidade();
	}

	@Test
	void clienteCriaPedidoComTotalDoServidorEReservaEstoque() throws Exception {
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("TOTEM", null, unidade.getId(), "[" + item(baiao.getId(), 2) + "," + item(suco.getId(), 1) + "]")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
				.andExpect(jsonPath("$.canalPedido").value("TOTEM"))
				.andExpect(jsonPath("$.clienteId").value(cliente.getId()))
				.andExpect(jsonPath("$.total").value(45.50)) // 2*20.00 (preço da unidade, não o base) + 5.50
				.andExpect(jsonPath("$.itens", hasSize(2)))
				.andExpect(jsonPath("$.itens[0].precoUnitario").value(20.00));
		assertEquals(3, saldo(baiao));
		assertEquals(4, saldo(suco));
	}

	@Test
	void itensRepetidosSaoSomados() throws Exception {
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("APP", null, unidade.getId(), "[" + item(suco.getId(), 1) + "," + item(suco.getId(), 2) + "]")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.itens", hasSize(1)))
				.andExpect(jsonPath("$.itens[0].quantidade").value(3));
	}

	@Test
	void canalPedidoAusenteRetorna422EInvalidoRetorna400() throws Exception {
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido(null, null, unidade.getId(), "[" + item(suco.getId(), 1) + "]")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.details[0].field").value("canalPedido"));
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("XYZ", null, unidade.getId(), "[" + item(suco.getId(), 1) + "]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("REQUISICAO_INVALIDA"));
	}

	@Test
	void itensInvalidosRetornam422() throws Exception {
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("APP", null, unidade.getId(), "[]")))
				.andExpect(status().isUnprocessableEntity());
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("APP", null, unidade.getId(), "[" + item(suco.getId(), -1) + "]")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.details[0].field").value("itens[0].quantidade"));
	}

	@Test
	void unidadeOuProdutoInexistenteRetorna404() throws Exception {
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("APP", null, 999999L, "[" + item(suco.getId(), 1) + "]")))
				.andExpect(status().isNotFound());
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("APP", null, unidade.getId(), "[" + item(999999L, 1) + "]")))
				.andExpect(status().isNotFound());
	}

	@Test
	void produtoForaDoCardapioOuIndisponivelRetorna409() throws Exception {
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("APP", null, unidade.getId(), "[" + item(fora.getId(), 1) + "]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("PRODUTO_INDISPONIVEL"));
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("APP", null, unidade.getId(), "[" + item(oculto.getId(), 1) + "]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("PRODUTO_INDISPONIVEL"));
	}

	@Test
	void estoqueInsuficienteRetorna409EDesfazTudo() throws Exception {
		// item 0 tem estoque; item 1 não: nada pode ser baixado
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("APP", null, unidade.getId(), "[" + item(baiao.getId(), 1) + "," + item(suco.getId(), 6) + "]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("ESTOQUE_INSUFICIENTE"))
				.andExpect(jsonPath("$.details[0].field").value("itens[1].quantidade"))
				.andExpect(jsonPath("$.details[0].issue").value("Disponível: 5"));
		assertEquals(5, saldo(baiao));
		assertEquals(5, saldo(suco));
		mvc.perform(get("/pedidos?unidadeId=" + unidade.getId()).header("Authorization", tokenCliente))
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void segurancaDaCriacao() throws Exception {
		String corpo = pedido("APP", null, unidade.getId(), "[" + item(suco.getId(), 1) + "]");
		mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/pedidos").header("Authorization", tokenPerfil("coz", Perfil.COZINHA))
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isForbidden());
		Usuario outro = usuarios.cadastrar("Outro", "outro" + seq + "@p.com", SENHA, Perfil.CLIENTE);
		mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("APP", outro.getId(), unidade.getId(), "[" + item(suco.getId(), 1) + "]")))
				.andExpect(status().isForbidden());
	}

	@Test
	void atendenteCriaPedidoDeBalcaoParaClienteOuSemCliente() throws Exception {
		String atendente = tokenPerfil("atd", Perfil.ATENDENTE);
		mvc.perform(post("/pedidos").header("Authorization", atendente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("BALCAO", cliente.getId(), unidade.getId(), "[" + item(suco.getId(), 1) + "]")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.clienteId").value(cliente.getId()));
		mvc.perform(post("/pedidos").header("Authorization", atendente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("BALCAO", null, unidade.getId(), "[" + item(suco.getId(), 1) + "]")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.clienteId").doesNotExist());
	}

	@Test
	void filtraPorCanalEStatusEClienteVeSoOsSeus() throws Exception {
		String atendente = tokenPerfil("atd", Perfil.ATENDENTE);
		for (String canal : new String[] { "TOTEM", "TOTEM", "APP" }) {
			mvc.perform(post("/pedidos").header("Authorization", tokenCliente).contentType(MediaType.APPLICATION_JSON)
					.content(pedido(canal, null, unidade.getId(), "[" + item(suco.getId(), 1) + "]")))
					.andExpect(status().isCreated());
		}
		mvc.perform(post("/pedidos").header("Authorization", atendente).contentType(MediaType.APPLICATION_JSON)
				.content(pedido("BALCAO", null, unidade.getId(), "[" + item(suco.getId(), 1) + "]")))
				.andExpect(status().isCreated());

		mvc.perform(get("/pedidos?canalPedido=TOTEM&unidadeId=" + unidade.getId()).header("Authorization", atendente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[*].canalPedido", everyItem(equalTo("TOTEM"))));
		mvc.perform(get("/pedidos?status=AGUARDANDO_PAGAMENTO&unidadeId=" + unidade.getId()).header("Authorization", atendente))
				.andExpect(jsonPath("$.totalElements").value(4));
		mvc.perform(get("/pedidos?status=PRONTO&unidadeId=" + unidade.getId()).header("Authorization", atendente))
				.andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/pedidos?canalPedido=XYZ").header("Authorization", atendente))
				.andExpect(status().isBadRequest());
		// cliente enxerga só os 3 criados por ele (o de balcão sem cliente fica de fora)
		mvc.perform(get("/pedidos?unidadeId=" + unidade.getId()).header("Authorization", tokenCliente))
				.andExpect(jsonPath("$.totalElements").value(3));
	}

	@Test
	void detalheDoPedidoERestritoAoDono() throws Exception {
		String corpo = mvc.perform(post("/pedidos").header("Authorization", tokenCliente)
				.contentType(MediaType.APPLICATION_JSON)
				.content(pedido("PICKUP", null, unidade.getId(), "[" + item(suco.getId(), 2) + "]")))
				.andReturn().getResponse().getContentAsString();
		long id = json.readTree(corpo).get("pedidoId").asLong();

		mvc.perform(get("/pedidos/" + id).header("Authorization", tokenCliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens[0].nome").value(suco.getNome()))
				.andExpect(jsonPath("$.total").value(11.00));
		usuarios.cadastrar("Intruso", "intruso" + seq + "@p.com", SENHA, Perfil.CLIENTE);
		mvc.perform(get("/pedidos/" + id).header("Authorization", token("intruso" + seq + "@p.com")))
				.andExpect(status().isNotFound());
		mvc.perform(get("/pedidos/" + id).header("Authorization", tokenPerfil("coz", Perfil.COZINHA)))
				.andExpect(status().isOk());
		mvc.perform(get("/pedidos/999999").header("Authorization", tokenCliente)).andExpect(status().isNotFound());
	}
}
