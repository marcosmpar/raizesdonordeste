package br.com.marcosmpar.raizesdonordeste.application.auth;

import br.com.marcosmpar.raizesdonordeste.domain.usuario.Usuario;

/** Porta para emissão de token de acesso (implementada na infraestrutura). */
public interface EmissorToken {

	String emitir(Usuario usuario);

	long expiracaoEmSegundos();
}
