package br.com.marcosmpar.raizesdonordeste.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import br.com.marcosmpar.raizesdonordeste.application.usuario.UsuarioService;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	UsuarioService usuarios;
	@Autowired
	JsonMapper json;

	private static final String SENHA = "Senha@123";

	private String json(String email, String senha) {
		return "{\"nome\":\"Teste\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
	}

	private String login(String email) throws Exception {
		String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		JsonNode n = json.readTree(body);
		return n.get("accessToken").asString();
	}

	@Test
	void cadastroDeClienteRetorna201SemExporSenha() throws Exception {
		mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(json("novo@t.com", SENHA)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.perfil").value("CLIENTE"))
				.andExpect(jsonPath("$.id").exists())
				.andExpect(content().string(not(containsString("senha"))));
	}

	@Test
	void cadastroComEmailDuplicadoRetorna409() throws Exception {
		usuarios.cadastrar("Dup", "dup@t.com", SENHA, Perfil.CLIENTE);
		mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(json("DUP@t.com", SENHA)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("EMAIL_JA_CADASTRADO"));
	}

	@Test
	void cadastroInvalidoRetorna422ComDetalhes() throws Exception {
		mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(json("nao-e-email", "123")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error").value("VALIDACAO_INVALIDA"))
				.andExpect(jsonPath("$.details.length()").value(2));
	}

	@Test
	void loginValidoRetornaToken() throws Exception {
		usuarios.cadastrar("Maria", "maria@t.com", SENHA, Perfil.CLIENTE);
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"maria@t.com\",\"senha\":\"" + SENHA + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.user.perfil").value("CLIENTE"));
	}

	@Test
	void loginComSenhaErradaRetorna401() throws Exception {
		usuarios.cadastrar("Ana", "ana@t.com", SENHA, Perfil.CLIENTE);
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ana@t.com\",\"senha\":\"errada123\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("CREDENCIAIS_INVALIDAS"));
	}

	@Test
	void semTokenRetorna401NoPadraoDeErro() throws Exception {
		mvc.perform(get("/usuarios/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().exists("X-Request-Id"))
				.andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"))
				.andExpect(jsonPath("$.path").value("/usuarios/me"))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.requestId").exists());
	}

	@Test
	void tokenInvalidoRetorna401() throws Exception {
		mvc.perform(get("/usuarios/me").header("Authorization", "Bearer abc.def.ghi"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meComTokenRetornaPerfil() throws Exception {
		usuarios.cadastrar("Bia", "bia@t.com", SENHA, Perfil.CLIENTE);
		mvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + login("bia@t.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("bia@t.com"));
	}

	@Test
	void clienteNaoCadastraUsuarioInterno403() throws Exception {
		usuarios.cadastrar("Cli", "cli@t.com", SENHA, Perfil.CLIENTE);
		mvc.perform(post("/usuarios/internos").header("Authorization", "Bearer " + login("cli@t.com"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"X\",\"email\":\"x@t.com\",\"senha\":\"" + SENHA + "\",\"perfil\":\"COZINHA\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
	}

	@Test
	void adminCadastraUsuarioInterno201() throws Exception {
		usuarios.cadastrar("Adm", "adm@t.com", SENHA, Perfil.ADMIN);
		mvc.perform(post("/usuarios/internos").header("Authorization", "Bearer " + login("adm@t.com"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Coz\",\"email\":\"coz@t.com\",\"senha\":\"" + SENHA + "\",\"perfil\":\"COZINHA\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.perfil").value("COZINHA"));
	}

	@Test
	void rotaInexistenteRetorna404NoPadraoDeErro() throws Exception {
		usuarios.cadastrar("Adm2", "adm2@t.com", SENHA, Perfil.ADMIN);
		mvc.perform(get("/nao-existe").header("Authorization", "Bearer " + login("adm2@t.com")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("ROTA_NAO_ENCONTRADA"));
	}
}
