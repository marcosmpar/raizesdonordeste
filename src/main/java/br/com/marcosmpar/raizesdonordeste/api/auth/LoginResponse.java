package br.com.marcosmpar.raizesdonordeste.api.auth;

import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserInfo user) {

	public record UserInfo(Long id, String nome, Perfil perfil) {
	}
}
