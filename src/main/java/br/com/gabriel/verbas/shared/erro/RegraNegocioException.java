package br.com.gabriel.verbas.shared.erro;

/** Violação de regra de negócio em estado consistente (HTTP 409). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
