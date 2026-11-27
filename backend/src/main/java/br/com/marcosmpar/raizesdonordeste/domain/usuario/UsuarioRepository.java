package br.com.marcosmpar.raizesdonordeste.domain.usuario;

import java.util.Optional;

/** Porta de persistência de usuários (implementada na infraestrutura). */
public interface UsuarioRepository {

	Usuario salvar(Usuario usuario);

	Optional<Usuario> buscarPorId(Long id);

	Optional<Usuario> buscarPorEmail(String email);

	boolean existePorEmail(String email);
}
