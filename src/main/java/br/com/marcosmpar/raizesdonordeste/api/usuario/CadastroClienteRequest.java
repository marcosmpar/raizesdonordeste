package br.com.marcosmpar.raizesdonordeste.api.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroClienteRequest(
		@Schema(example = "Maria Silva") @NotBlank @Size(max = 120) String nome,
		@Schema(example = "maria@exemplo.com") @NotBlank @Email @Size(max = 160) String email,
		@Schema(example = "Senha@123", description = "Mínimo 8 caracteres") @NotBlank @Size(min = 8, max = 72) String senha) {
}
