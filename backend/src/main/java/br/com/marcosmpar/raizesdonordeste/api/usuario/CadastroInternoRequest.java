package br.com.marcosmpar.raizesdonordeste.api.usuario;

import br.com.marcosmpar.raizesdonordeste.domain.usuario.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroInternoRequest(
		@Schema(example = "João Cozinha") @NotBlank @Size(max = 120) String nome,
		@Schema(example = "joao@raizes.com") @NotBlank @Email @Size(max = 160) String email,
		@Schema(example = "Senha@123") @NotBlank @Size(min = 8, max = 72) String senha,
		@Schema(example = "COZINHA") @NotNull Perfil perfil) {
}
