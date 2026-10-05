package br.com.gabriel.verbas.departamento.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DepartamentoRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
        String nome,

        @NotNull(message = "orcamentoTotal é obrigatório")
        @Positive(message = "orcamentoTotal deve ser positivo")
        @Digits(integer = 12, fraction = 2, message = "orcamentoTotal deve ter no máximo 12 dígitos inteiros e 2 decimais")
        BigDecimal orcamentoTotal) {
}
