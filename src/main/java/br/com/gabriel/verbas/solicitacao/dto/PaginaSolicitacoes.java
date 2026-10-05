package br.com.gabriel.verbas.solicitacao.dto;

import java.util.List;

public record PaginaSolicitacoes(
        List<SolicitacaoResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
