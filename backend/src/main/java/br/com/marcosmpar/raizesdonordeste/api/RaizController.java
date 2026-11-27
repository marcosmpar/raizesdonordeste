package br.com.marcosmpar.raizesdonordeste.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Hidden;

/** A raiz do site leva direto à documentação interativa (Swagger UI). */
@Controller
@Hidden
public class RaizController {

	@GetMapping("/")
	public String raiz() {
		return "redirect:/swagger-ui.html";
	}
}
