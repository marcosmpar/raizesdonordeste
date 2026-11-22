package br.com.marcosmpar.raizesdonordeste.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RaizRedirectTest {

	@Autowired
	MockMvc mvc;

	@Test
	void raizRedirecionaParaOSwaggerSemExigirLogin() throws Exception {
		mvc.perform(get("/")).andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", "/swagger-ui.html"));
	}
}
