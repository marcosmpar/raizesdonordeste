package br.com.marcosmpar.raizesdonordeste.application.pedido;

import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;

/** Quem está executando a operação (derivado do token). */
public record Solicitante(Long id, Perfil perfil) {
}
