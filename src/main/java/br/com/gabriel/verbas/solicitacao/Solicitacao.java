package br.com.gabriel.verbas.solicitacao;

import br.com.gabriel.verbas.shared.Dinheiro;

import br.com.gabriel.verbas.departamento.Departamento;
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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "solicitacao")
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;

    @Column(name = "descricao", nullable = false, length = 500)
    private String descricao;

    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusSolicitacao status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Solicitacao() {
    }

    public Solicitacao(Departamento departamento, String descricao, BigDecimal valor) {
        Dinheiro.validarPositivo(valor, "valor");
        if (departamento == null) {
            throw new IllegalArgumentException("departamento e obrigatorio");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("descricao e obrigatoria");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
        this.departamento = departamento;
        this.descricao = descricao;
        this.valor = valor;
        this.status = StatusSolicitacao.PENDENTE;
        this.criadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public boolean isPendente() {
        return status == StatusSolicitacao.PENDENTE;
    }

    /** Transicao PENDENTE -> APROVADA. A reserva do saldo e responsabilidade do servico. */
    public void aprovar() {
    	if (!isPendente()) {
            throw new IllegalStateException("apenas solicitacoes PENDENTES podem ser aprovadas");
        }
        this.status = StatusSolicitacao.APROVADA;
    }

    /** Transicao PENDENTE -> REJEITADA. Nao altera saldo. */
    public void rejeitar() {
        if (!isPendente()) {
            throw new IllegalStateException("apenas solicitacoes PENDENTES podem ser rejeitadas");
        }
        this.status = StatusSolicitacao.REJEITADA;
    }
}
