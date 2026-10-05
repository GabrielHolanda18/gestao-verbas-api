package br.com.gabriel.verbas.shared.erro;

import java.util.List;

/** Entrada inválida (HTTP 400), com erros de campo opcionais. */
public class ValidacaoException extends RuntimeException {

    private final List<String> erros;

    public ValidacaoException(String mensagem) {
        this(mensagem, List.of());
    }

    public ValidacaoException(String mensagem, List<String> erros) {
        super(mensagem);
        this.erros = erros == null ? List.of() : List.copyOf(erros);
    }

    public List<String> getErros() {
        return erros;
    }
}
