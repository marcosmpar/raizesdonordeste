package br.com.marcosmpar.raizesdonordeste.application.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.marcosmpar.raizesdonordeste.domain.exception.CredenciaisInvalidasException;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.UsuarioRepository;

@Service
public class AutenticacaoService {

	public record Resultado(String accessToken, long expiresIn, Usuario usuario) {
	}

	private final UsuarioRepository usuarios;
	private final PasswordEncoder encoder;
	private final EmissorToken emissor;

	public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder encoder, EmissorToken emissor) {
		this.usuarios = usuarios;
		this.encoder = encoder;
		this.emissor = emissor;
	}

	public Resultado login(String email, String senha) {
		// Mesma resposta para e-mail inexistente, senha errada ou usuário inativo (não vaza qual falhou)
		Usuario usuario = usuarios.buscarPorEmail(email)
				.filter(Usuario::isAtivo)
				.filter(u -> encoder.matches(senha, u.getSenhaHash()))
				.orElseThrow(CredenciaisInvalidasException::new);
		return new Resultado(emissor.emitir(usuario), emissor.expiracaoEmSegundos(), usuario);
	}
}
