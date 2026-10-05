package br.com.gabriel.verbas.solicitacao.dto;

import br.com.gabriel.verbas.solicitacao.Solicitacao;

import java.math.BigDecimal;
import java.time.Instant;

public record SolicitacaoResponse(
        Long id,
        Long departamentoId,
        String descricao,
        BigDecimal valor,
        String status,
        Instant criadoEm) {

    public static SolicitacaoResponse from(Solicitacao s) {
        return new SolicitacaoResponse(s.getId(), s.getDepartamento().getId(), s.getDescricao(),
                s.getValor(), s.getStatus().name(), s.getCriadoEm());
    }
}
