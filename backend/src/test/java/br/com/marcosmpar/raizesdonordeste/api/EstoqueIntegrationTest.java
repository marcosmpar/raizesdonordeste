package br.com.marcosmpar.raizesdonordeste.api;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.com.marcosmpar.raizesdonordeste.application.estoque.EstoqueService;
import br.com.marcosmpar.raizesdonordeste.application.produto.ProdutoService;
import br.com.marcosmpar.raizesdonordeste.application.unidade.UnidadeService;
import br.com.marcosmpar.raizesdonordeste.application.usuario.UsuarioService;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstoqueIntegrationTest {

	private static final String SENHA = "Senha@123";

	@Autowired MockMvc mvc;
	@Autowired UsuarioService usuarios;
	@Autowired UnidadeService unidades;
	@Autowired ProdutoService produtos;
	@Autowired EstoqueService estoque;
	@Autowired JsonMapper json;

	private String token(String email, Perfil perfil) throws Exception {
		try {
			usuarios.cadastrar("U", email, SENHA, perfil);
		}
		catch (RuntimeException ignorado) {
			// já existe (criado por outro teste)
		}
		String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + json.readTree(body).get("accessToken").asString();
	}

	private String corpo(Long unidadeId, Long produtoId, int qtd, String motivo) {
		return "{\"unidadeId\":%d,\"produtoId\":%d,\"quantidade\":%d,\"motivo\":\"%s\"}".formatted(unidadeId,
				produtoId, qtd, motivo);
	}

	@Test
	void entradaESaidaAtualizamSaldoEMovimentacoes() throws Exception {
		String auth = token("ger.est@t.com", Perfil.GERENTE);
		var u = unidades.criar("Estoque U1", "Recife", "PE", "Rua 1", true);
		var p = produtos.criar("Produto E1", null, "Pratos", new BigDecimal("10.00"));

		mvc.perform(post("/estoque/entradas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(u.getId(), p.getId(), 20, "Reposição")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("ENTRADA"))
				.andExpect(jsonPath("$.usuarioId").isNumber());
		mvc.perform(post("/estoque/saidas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(u.getId(), p.getId(), 5, "Perda")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("SAIDA"));

		mvc.perform(get("/estoque?unidadeId=" + u.getId()).header("Authorization", auth))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].quantidade").value(15))
				.andExpect(jsonPath("$.content[0].produto").value("Produto E1"));
		mvc.perform(get("/estoque/movimentacoes?unidadeId=" + u.getId() + "&produtoId=" + p.getId())
				.header("Authorization", auth))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[0].tipo").value("SAIDA"));
	}

	@Test
	void estoqueEPorUnidade() throws Exception {
		String auth = token("ger.est@t.com", Perfil.GERENTE);
		var u1 = unidades.criar("Estoque U2a", "Recife", "PE", "Rua 1", true);
		var u2 = unidades.criar("Estoque U2b", "Olinda", "PE", "Rua 2", true);
		var p = produtos.criar("Produto E2", null, "Pratos", new BigDecimal("10.00"));
		estoque.registrarEntrada(u1.getId(), p.getId(), 10, null, null);

		mvc.perform(get("/estoque?unidadeId=" + u2.getId()).header("Authorization", auth))
				.andExpect(jsonPath("$.totalElements").value(0));
		// sem saldo na u2: saída é recusada mesmo havendo saldo na u1
		mvc.perform(post("/estoque/saidas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(u2.getId(), p.getId(), 1, "Teste")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.details[0].issue").value("Disponível: 0"));
	}

	@Test
	void saidaMaiorQueSaldoRetorna409EMantemSaldo() throws Exception {
		String auth = token("ger.est@t.com", Perfil.GERENTE);
		var u = unidades.criar("Estoque U3", "Recife", "PE", "Rua 1", true);
		var p = produtos.criar("Produto E3", null, "Pratos", new BigDecimal("10.00"));
		estoque.registrarEntrada(u.getId(), p.getId(), 3, null, null);

		mvc.perform(post("/estoque/saidas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(u.getId(), p.getId(), 4, "Excesso")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("ESTOQUE_INSUFICIENTE"))
				.andExpect(jsonPath("$.details[0].issue").value("Disponível: 3"));
		mvc.perform(get("/estoque?unidadeId=" + u.getId()).header("Authorization", auth))
				.andExpect(jsonPath("$.content[0].quantidade").value(3));
	}

	@Test
	void validacoesEPermissoes() throws Exception {
		String ger = token("ger.est@t.com", Perfil.GERENTE);
		var u = unidades.criar("Estoque U4", "Recife", "PE", "Rua 1", true);
		var p = produtos.criar("Produto E4", null, "Pratos", new BigDecimal("10.00"));

		mvc.perform(post("/estoque/entradas").header("Authorization", ger).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(u.getId(), p.getId(), 0, "x")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.details[0].field").value("quantidade"));
		mvc.perform(post("/estoque/saidas").header("Authorization", ger).contentType(MediaType.APPLICATION_JSON)
				.content("{\"unidadeId\":1,\"produtoId\":1,\"quantidade\":1}"))
				.andExpect(status().isUnprocessableEntity());
		mvc.perform(post("/estoque/entradas").header("Authorization", ger).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(999999L, p.getId(), 1, "x")))
				.andExpect(status().isNotFound());
		mvc.perform(post("/estoque/entradas").header("Authorization", ger).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(u.getId(), 999999L, 1, "x")))
				.andExpect(status().isNotFound());
		mvc.perform(post("/estoque/entradas").header("Authorization", token("cli.est@t.com", Perfil.CLIENTE))
				.contentType(MediaType.APPLICATION_JSON).content(corpo(u.getId(), p.getId(), 1, "x")))
				.andExpect(status().isForbidden());
		mvc.perform(get("/estoque?unidadeId=" + u.getId())).andExpect(status().isUnauthorized());
		mvc.perform(get("/estoque?unidadeId=" + u.getId()).header("Authorization", token("coz.est@t.com", Perfil.COZINHA)))
				.andExpect(status().isForbidden());
	}

	@Test
	void saidasConcorrentesNuncaDeixamSaldoNegativo() throws Exception {
		var u = unidades.criar("Estoque U5", "Recife", "PE", "Rua 1", true);
		var p = produtos.criar("Produto E5", null, "Pratos", new BigDecimal("10.00"));
		estoque.registrarEntrada(u.getId(), p.getId(), 5, null, null);

		ExecutorService pool = Executors.newFixedThreadPool(10);
		List<Future<Boolean>> resultados = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Callable<Boolean> tarefa = () -> {
				try {
					estoque.registrarSaida(u.getId(), p.getId(), 1, "concorrente", null);
					return true;
				}
				catch (RegraNegocioException e) {
					return false;
				}
			};
			resultados.add(pool.submit(tarefa));
		}
		int sucessos = 0;
		for (Future<Boolean> f : resultados) {
			if (f.get()) {
				sucessos++;
			}
		}
		pool.shutdown();

		assertEquals(5, sucessos);
		assertEquals(0, estoque.saldos(u.getId(), 1, 10).conteudo().get(0).getQuantidade());
	}
}
