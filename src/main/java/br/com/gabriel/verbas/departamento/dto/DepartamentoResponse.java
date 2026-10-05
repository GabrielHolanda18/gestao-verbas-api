package br.com.gabriel.verbas.departamento.dto;

import br.com.gabriel.verbas.departamento.Departamento;

import java.math.BigDecimal;
import java.time.Instant;

public record DepartamentoResponse(
        Long id,
        String nome,
        BigDecimal orcamentoTotal,
        BigDecimal valorComprometido,
        BigDecimal saldoDisponivel,
        Instant criadoEm) {

    public static DepartamentoResponse from(Departamento d) {
        return new DepartamentoResponse(d.getId(), d.getNome(), d.getOrcamentoTotal(),
                d.getValorComprometido(), d.saldoDisponivel(), d.getCriadoEm());
    }
}
