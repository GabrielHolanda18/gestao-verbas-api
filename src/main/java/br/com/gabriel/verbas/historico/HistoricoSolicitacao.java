package br.com.gabriel.verbas.historico;

import br.com.gabriel.verbas.solicitacao.Solicitacao;
import br.com.gabriel.verbas.solicitacao.StatusSolicitacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "historico_solicitacao")
public class HistoricoSolicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id")
    private Solicitacao solicitacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", nullable = false, length = 20)
    private StatusSolicitacao statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusSolicitacao statusNovo;

    @Column(name = "responsavel", nullable = false, length = 120)
    private String responsavel;

    @Column(name = "justificativa", length = 500)
    private String justificativa;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected HistoricoSolicitacao() {
    }

    public HistoricoSolicitacao(Solicitacao solicitacao, StatusSolicitacao statusAnterior,
            StatusSolicitacao statusNovo, String responsavel, String justificativa) {
        if (solicitacao == null) {
            throw new IllegalArgumentException("solicitacao e obrigatoria");
        }
        if (statusAnterior == null || statusNovo == null) {
            throw new IllegalArgumentException("status anterior e novo sao obrigatorios");
        }
        if (responsavel == null || responsavel.isBlank()) {
            throw new IllegalArgumentException("responsavel e obrigatorio");
        }
        if (statusNovo == StatusSolicitacao.REJEITADA && (justificativa == null || justificativa.isBlank())) {
            throw new IllegalArgumentException("justificativa e obrigatoria para rejeicao");
        }
        this.solicitacao = solicitacao;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.responsavel = responsavel;
        this.justificativa = justificativa;
        this.criadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Solicitacao getSolicitacao() {
        return solicitacao;
    }

    public StatusSolicitacao getStatusAnterior() {
        return statusAnterior;
    }

    public StatusSolicitacao getStatusNovo() {
        return statusNovo;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
