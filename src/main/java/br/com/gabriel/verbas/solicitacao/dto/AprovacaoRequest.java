package br.com.gabriel.verbas.solicitacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AprovacaoRequest(
        @NotBlank(message = "responsavel é obrigatório")
        @Size(max = 120, message = "responsavel deve ter no máximo 120 caracteres")
        String responsavel) {
}
