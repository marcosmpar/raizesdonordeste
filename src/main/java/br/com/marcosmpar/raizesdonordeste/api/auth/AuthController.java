package br.com.marcosmpar.raizesdonordeste.api.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.application.auth.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth")
public class AuthController {

	private final AutenticacaoService service;

	public AuthController(AutenticacaoService service) {
		this.service = service;
	}

	@Operation(summary = "Autentica e retorna o token JWT")
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest req) {
		var r = service.login(req.email(), req.senha());
		return new LoginResponse(r.accessToken(), "Bearer", r.expiresIn(),
				new LoginResponse.UserInfo(r.usuario().getId(), r.usuario().getNome(), r.usuario().getPerfil()));
	}
}
