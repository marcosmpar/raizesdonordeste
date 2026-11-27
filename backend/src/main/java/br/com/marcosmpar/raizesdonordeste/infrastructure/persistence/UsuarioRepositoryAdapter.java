package br.com.marcosmpar.raizesdonordeste.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.UsuarioRepository;

@Repository
class UsuarioRepositoryAdapter implements UsuarioRepository {

	private final UsuarioJpaRepository jpa;

	UsuarioRepositoryAdapter(UsuarioJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Usuario salvar(Usuario usuario) {
		return jpa.save(usuario);
	}

	@Override
	public Optional<Usuario> buscarPorId(Long id) {
		return jpa.findById(id);
	}

	@Override
	public Optional<Usuario> buscarPorEmail(String email) {
		return jpa.findByEmail(Usuario.normalizarEmail(email));
	}

	@Override
	public boolean existePorEmail(String email) {
		return jpa.existsByEmail(Usuario.normalizarEmail(email));
	}
}
