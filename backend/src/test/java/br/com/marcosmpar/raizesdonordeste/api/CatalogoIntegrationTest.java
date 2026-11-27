package br.com.marcosmpar.raizesdonordeste.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.com.marcosmpar.raizesdonordeste.application.cardapio.CardapioService;
import br.com.marcosmpar.raizesdonordeste.application.produto.ProdutoService;
import br.com.marcosmpar.raizesdonordeste.application.unidade.UnidadeService;
import br.com.marcosmpar.raizesdonordeste.application.usuario.UsuarioService;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogoIntegrationTest {

	private static final String SENHA = "Senha@123";
	private static final String JSON_UNIDADE = "{\"nome\":\"%s\",\"cidade\":\"Recife\",\"uf\":\"PE\",\"endereco\":\"Rua A, 1\"}";
	private static final String JSON_PRODUTO = "{\"nome\":\"%s\",\"categoria\":\"Pratos\",\"precoBase\":%s}";

	@Autowired MockMvc mvc;
	@Autowired UsuarioService usuarios;
	@Autowired UnidadeService unidades;
	@Autowired ProdutoService produtos;
	@Autowired CardapioService cardapio;
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

	@Test
	void listagemPublicaDeUnidadesEPaginada() throws Exception {
		unidades.criar("AAA Unidade Pública", "Recife", "PE", "Rua X", true);
		mvc.perform(get("/unidades?page=1&limit=5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].nome", hasItem("AAA Unidade Pública")))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.limit").value(5));
	}

	@Test
	void paginacaoInvalidaRetorna422() throws Exception {
		mvc.perform(get("/unidades?page=0&limit=500"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error").value("VALIDACAO_INVALIDA"));
	}

	@Test
	void adminCriaUnidadeEClienteNaoPode() throws Exception {
		mvc.perform(post("/unidades").header("Authorization", token("adm.cat@t.com", Perfil.ADMIN))
				.contentType(MediaType.APPLICATION_JSON).content(JSON_UNIDADE.formatted("Nova Unidade")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ativa").value(true));
		mvc.perform(post("/unidades").header("Authorization", token("cli.cat@t.com", Perfil.CLIENTE))
				.contentType(MediaType.APPLICATION_JSON).content(JSON_UNIDADE.formatted("Outra")))
				.andExpect(status().isForbidden());
		mvc.perform(post("/unidades").contentType(MediaType.APPLICATION_JSON).content(JSON_UNIDADE.formatted("Outra")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unidadeDuplicadaRetorna409() throws Exception {
		unidades.criar("Duplicada", "Recife", "PE", "Rua", true);
		mvc.perform(post("/unidades").header("Authorization", token("adm.cat@t.com", Perfil.ADMIN))
				.contentType(MediaType.APPLICATION_JSON).content(JSON_UNIDADE.formatted("duplicada")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("UNIDADE_JA_CADASTRADA"));
	}

	@Test
	void unidadeInexistenteRetorna404() throws Exception {
		mvc.perform(get("/unidades/999999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("RECURSO_NAO_ENCONTRADO"));
		mvc.perform(get("/unidades/999999/cardapio")).andExpect(status().isNotFound());
	}

	@Test
	void produtoComPrecoInvalidoRetorna422() throws Exception {
		mvc.perform(post("/produtos").header("Authorization", token("ger.cat@t.com", Perfil.GERENTE))
				.contentType(MediaType.APPLICATION_JSON).content(JSON_PRODUTO.formatted("Ruim", "-5")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.details[0].field").value("precoBase"));
	}

	@Test
	void gerenteCriaProdutoEDesativaComSoftDelete() throws Exception {
		String auth = token("ger.cat@t.com", Perfil.GERENTE);
		mvc.perform(post("/produtos").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
				.content(JSON_PRODUTO.formatted("Feijoada Nordestina", "59.90")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.precoBase").value(59.90));
		Long id = produtos.listar(1, 100).conteudo().stream()
				.filter(p -> p.getNome().equals("Feijoada Nordestina")).findFirst().orElseThrow().getId();
		mvc.perform(delete("/produtos/" + id).header("Authorization", auth)).andExpect(status().isNoContent());
		mvc.perform(get("/produtos/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
		mvc.perform(get("/produtos?limit=100")).andExpect(jsonPath("$.content[*].nome", not(hasItem("Feijoada Nordestina"))));
	}

	@Test
	void cardapioPorUnidadeMostraSoDisponiveisComPrecoDaUnidade() throws Exception {
		String auth = token("ger.cat@t.com", Perfil.GERENTE);
		var u1 = unidades.criar("Cardápio U1", "Recife", "PE", "Rua 1", true);
		var u2 = unidades.criar("Cardápio U2", "Olinda", "PE", "Rua 2", true);
		var disponivel = produtos.criar("Tapioca C", null, "Lanches", new BigDecimal("10.00"));
		var indisponivel = produtos.criar("Cuscuz C", null, "Lanches", new BigDecimal("12.00"));

		mvc.perform(put("/unidades/" + u1.getId() + "/cardapio/" + disponivel.getId()).header("Authorization", auth)
				.contentType(MediaType.APPLICATION_JSON).content("{\"preco\":11.50,\"disponivel\":true}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.preco").value(11.50));
		// upsert: segunda chamada atualiza o preço
		mvc.perform(put("/unidades/" + u1.getId() + "/cardapio/" + disponivel.getId()).header("Authorization", auth)
				.contentType(MediaType.APPLICATION_JSON).content("{\"preco\":13.00,\"disponivel\":true}"))
				.andExpect(status().isOk());
		cardapio.definir(u1.getId(), indisponivel.getId(), new BigDecimal("12.00"), false);

		mvc.perform(get("/unidades/" + u1.getId() + "/cardapio"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].nome").value("Tapioca C"))
				.andExpect(jsonPath("$.content[0].preco").value(13.00));
		// cardápio é por unidade: a outra unidade não tem itens
		mvc.perform(get("/unidades/" + u2.getId() + "/cardapio"))
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void cardapioComProdutoInexistenteRetorna404EClienteRecebe403() throws Exception {
		var u = unidades.criar("Cardápio U3", "Recife", "PE", "Rua 3", true);
		mvc.perform(put("/unidades/" + u.getId() + "/cardapio/999999").header("Authorization", token("ger.cat@t.com", Perfil.GERENTE))
				.contentType(MediaType.APPLICATION_JSON).content("{\"preco\":10,\"disponivel\":true}"))
				.andExpect(status().isNotFound());
		mvc.perform(put("/unidades/" + u.getId() + "/cardapio/1").header("Authorization", token("cli.cat@t.com", Perfil.CLIENTE))
				.contentType(MediaType.APPLICATION_JSON).content("{\"preco\":10,\"disponivel\":true}"))
				.andExpect(status().isForbidden());
	}
}
