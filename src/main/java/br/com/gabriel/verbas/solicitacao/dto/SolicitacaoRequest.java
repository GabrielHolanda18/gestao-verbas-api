package br.com.gabriel.verbas.solicitacao.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SolicitacaoRequest(
        @NotBlank(message = "descricao é obrigatória")
        @Size(max = 500, message = "descricao deve ter no máximo 500 caracteres")
        String descricao,

        @NotNull(message = "valor é obrigatório")
        @Positive(message = "valor deve ser positivo")
        @Digits(integer = 12, fraction = 2, message = "valor deve ter no máximo 12 dígitos inteiros e 2 decimais")
        BigDecimal valor) {
}
