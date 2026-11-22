package br.com.marcosmpar.raizesdonordeste.application.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcosmpar.raizesdonordeste.application.auditoria.AuditoriaService;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RecursoNaoEncontradoException;
import br.com.marcosmpar.raizesdonordeste.domain.exception.RegraNegocioException;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.UsuarioRepository;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarios;
	private final PasswordEncoder encoder;
	private final AuditoriaService auditoria;

	public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder, AuditoriaService auditoria) {
		this.usuarios = usuarios;
		this.encoder = encoder;
		this.auditoria = auditoria;
	}

	public Usuario cadastrar(String nome, String email, String senha, Perfil perfil) {
		return cadastrar(nome, email, senha, perfil, null);
	}

	/** atorId: quem criou a conta (nulo = autocadastro). */
	@Transactional
	public Usuario cadastrar(String nome, String email, String senha, Perfil perfil, Long atorId) {
		if (usuarios.existePorEmail(email)) {
			throw new RegraNegocioException("EMAIL_JA_CADASTRADO", "Já existe um usuário com este e-mail.");
		}
		Usuario u = usuarios.salvar(new Usuario(nome, email, encoder.encode(senha), perfil));
		auditoria.registrar(atorId, "USUARIO_CRIADO", "USUARIO", u.getId(), "perfil=" + perfil);
		return u;
	}

	@Transactional(readOnly = true)
	public Usuario buscar(Long id) {
		return usuarios.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
	}
}
