package br.com.marcosmpar.raizesdonordeste.infrastructure.security;

import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;

/** Principal colocado no SecurityContext após validar o JWT. */
public record UsuarioAutenticado(Long id, String email, Perfil perfil) {
}
