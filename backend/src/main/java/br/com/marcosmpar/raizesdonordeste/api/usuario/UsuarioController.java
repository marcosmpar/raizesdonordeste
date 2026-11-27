package br.com.marcosmpar.raizesdonordeste.api.usuario;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.marcosmpar.raizesdonordeste.application.usuario.UsuarioService;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import br.com.marcosmpar.raizesdonordeste.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários")
public class UsuarioController {

	private final UsuarioService service;

	public UsuarioController(UsuarioService service) {
		this.service = service;
	}

	@Operation(summary = "Cadastro público de cliente (perfil CLIENTE)")
	@PostMapping
	public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroClienteRequest req) {
		var u = service.cadastrar(req.nome(), req.email(), req.senha(), Perfil.CLIENTE);
		return ResponseEntity.created(URI.create("/usuarios/" + u.getId())).body(UsuarioResponse.de(u));
	}

	@Operation(summary = "Cadastro de usuário interno com perfil (somente ADMIN)",
			security = @SecurityRequirement(name = "bearer-jwt"))
	@PostMapping("/internos")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UsuarioResponse> cadastrarInterno(@Valid @RequestBody CadastroInternoRequest req,
			@AuthenticationPrincipal UsuarioAutenticado atual) {
		var u = service.cadastrar(req.nome(), req.email(), req.senha(), req.perfil(), atual.id());
		return ResponseEntity.created(URI.create("/usuarios/" + u.getId())).body(UsuarioResponse.de(u));
	}

	@Operation(summary = "Perfil do usuário autenticado", security = @SecurityRequirement(name = "bearer-jwt"))
	@GetMapping("/me")
	public UsuarioResponse me(@AuthenticationPrincipal UsuarioAutenticado atual) {
		return UsuarioResponse.de(service.buscar(atual.id()));
	}
}
