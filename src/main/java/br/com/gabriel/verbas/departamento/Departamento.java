package br.com.gabriel.verbas.departamento;

import br.com.gabriel.verbas.shared.Dinheiro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "departamento")
public class Departamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "orcamento_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal orcamentoTotal;

    @Column(name = "valor_comprometido", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorComprometido;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Departamento() {
    }

    public Departamento(String nome, BigDecimal orcamentoTotal) {
        Dinheiro.validarPositivo(orcamentoTotal, "orcamentoTotal");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome do departamento e obrigatoria");
        }
        if (orcamentoTotal == null || orcamentoTotal.signum() <= 0) {
            throw new IllegalArgumentException("orcamento total deve ser positivo");
        }
        this.nome = nome;
        this.orcamentoTotal = orcamentoTotal;
        this.valorComprometido = BigDecimal.ZERO;
        this.criadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getOrcamentoTotal() {
        return orcamentoTotal;
    }

    public BigDecimal getValorComprometido() {
        return valorComprometido;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public BigDecimal saldoDisponivel() {
        return orcamentoTotal.subtract(valorComprometido);
    }

    public boolean temSaldoPara(BigDecimal valor) {
        return valor != null && valor.signum() > 0 && saldoDisponivel().compareTo(valor) >= 0;
    }

    /** Reserva valor do orcamento. Deve ser chamado apenas com bloqueio pessimista ativo. */
    public void reservar(BigDecimal valor) {
        Dinheiro.validarPositivo(valor, "valor");
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("valor da reserva deve ser positivo");
        }
        if (!temSaldoPara(valor)) {
            throw new IllegalStateException("saldo insuficiente para aprovar a solicitacao");
        }
        this.valorComprometido = valorComprometido.add(valor);
    }
}
