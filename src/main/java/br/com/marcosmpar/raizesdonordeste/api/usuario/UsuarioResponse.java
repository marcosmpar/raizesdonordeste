package br.com.marcosmpar.raizesdonordeste.api.usuario;

import java.time.Instant;

import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;

/** Nunca expõe senha/hash. */
public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, Instant criadoEm) {

	public static UsuarioResponse de(Usuario u) {
		return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.getCriadoEm());
	}
}
