package br.com.marcosmpar.raizesdonordeste.domain.usuario;

public enum Perfil {
	CLIENTE, ATENDENTE, COZINHA, GERENTE, ADMIN;

	/** Nome da authority no Spring Security (ex.: ROLE_ADMIN). */
	public String authority() {
		return "ROLE_" + name();
	}
}
