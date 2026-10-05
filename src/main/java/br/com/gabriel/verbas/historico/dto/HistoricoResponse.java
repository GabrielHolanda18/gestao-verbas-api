package br.com.gabriel.verbas.historico.dto;

import br.com.gabriel.verbas.historico.HistoricoSolicitacao;

import java.time.Instant;

public record HistoricoResponse(
        Long id,
        Long solicitacaoId,
        String statusAnterior,
        String statusNovo,
        String responsavel,
        String justificativa,
        Instant criadoEm) {

    public static HistoricoResponse from(HistoricoSolicitacao h) {
        return new HistoricoResponse(h.getId(), h.getSolicitacao().getId(),
                h.getStatusAnterior().name(), h.getStatusNovo().name(),
                h.getResponsavel(), h.getJustificativa(), h.getCriadoEm());
    }
}
