package br.com.gabriel.verbas.shared;

import java.math.BigDecimal;

/** Utilitário de validação estrita de valores monetários: sem nulos, sem arredondamento. */
public final class Dinheiro {

    public static final BigDecimal MAXIMO = new BigDecimal("999999999999.99");

    private Dinheiro() {
    }

    /** Rejeita nulo, não positivo, mais de 2 casas decimais (inclusive zeros excedentes) e acima do máximo. */
    public static void validarPositivo(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " e obrigatorio");
        }
        if (valor.scale() > 2) {
            throw new IllegalArgumentException(campo + " deve ter no maximo 2 casas decimais");
        }
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(campo + " deve ser positivo");
        }
        if (valor.compareTo(MAXIMO) > 0) {
            throw new IllegalArgumentException(campo + " nao pode exceder 999999999999.99");
        }
    }
}
