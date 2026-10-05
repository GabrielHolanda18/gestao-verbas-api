package br.com.gabriel.verbas;

import br.com.gabriel.verbas.departamento.Departamento;
import br.com.gabriel.verbas.shared.Dinheiro;
import br.com.gabriel.verbas.solicitacao.Solicitacao;
import br.com.gabriel.verbas.solicitacao.StatusSolicitacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DominioTest {

    private static final BigDecimal MIL = new BigDecimal("1000.00");

    @Test
    void deveCriarDepartamentoComOrcamentoPositivoESaldoInicialIgualAoOrcamento() {
        Departamento departamento = new Departamento("TI", MIL);
        assertEquals("TI", departamento.getNome());
        assertEquals(0, departamento.getOrcamentoTotal().compareTo(MIL));
        assertEquals(0, departamento.getValorComprometido().compareTo(BigDecimal.ZERO));
        assertEquals(0, departamento.saldoDisponivel().compareTo(MIL));
    }

    @Test
    void deveRejeitarValorMonetarioComMaisDeDuasCasasDecimaisMesmoComZerosExcedentes() {
        assertThrows(IllegalArgumentException.class,
                () -> Dinheiro.validarPositivo(new BigDecimal("1.000"), "valor"));
        assertThrows(IllegalArgumentException.class,
                () -> Dinheiro.validarPositivo(new BigDecimal("10.12345"), "valor"));
    }

    @Test
    void deveRejeitarValorMonetarioZeroOuNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> Dinheiro.validarPositivo(BigDecimal.ZERO, "valor"));
        assertThrows(IllegalArgumentException.class,
                () -> Dinheiro.validarPositivo(new BigDecimal("-1.00"), "valor"));
    }

    @Test
    void naoDeveReservarQuandoSaldoInsuficienteMantendoValorComprometidoInalterado() {
        Departamento departamento = new Departamento("RH", new BigDecimal("100.00"));
        assertThrows(IllegalStateException.class,
                () -> departamento.reservar(new BigDecimal("200.00")));
        assertEquals(0, departamento.getValorComprometido().compareTo(BigDecimal.ZERO));
        assertEquals(0, departamento.saldoDisponivel().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void deveReservarNormalmenteAtualizandoComprometido() {
        Departamento departamento = new Departamento("RH", new BigDecimal("100.00"));
        departamento.reservar(new BigDecimal("40.00"));
        assertEquals(0, departamento.getValorComprometido().compareTo(new BigDecimal("40.00")));
        assertTrue(departamento.temSaldoPara(new BigDecimal("60.00")));
        assertFalse(departamento.temSaldoPara(new BigDecimal("60.01")));
    }

    @Test
    void deveCalcularSaldoDisponivelAposReservas() {
        Departamento departamento = new Departamento("Engenharia", MIL);
        departamento.reservar(new BigDecimal("250.50"));
        departamento.reservar(new BigDecimal("149.50"));
        assertEquals(0, departamento.saldoDisponivel().compareTo(new BigDecimal("600.00")));
    }

    @Test
    void solicitacaoNascePendenteEAprovacaoAlteraStatus() {
        Departamento departamento = new Departamento("TI", MIL);
        Solicitacao solicitacao = new Solicitacao(departamento, "Compra de licenças", new BigDecimal("100.00"));
        assertTrue(solicitacao.isPendente());
        assertEquals(StatusSolicitacao.PENDENTE, solicitacao.getStatus());
        solicitacao.aprovar();
        assertFalse(solicitacao.isPendente());
        assertEquals(StatusSolicitacao.APROVADA, solicitacao.getStatus());
    }

    @Test
    void segundaDecisaoTerminalDeveSerRejeitada() {
        Departamento departamento = new Departamento("TI", MIL);
        Solicitacao solicitacao = new Solicitacao(departamento, "Notebook", new BigDecimal("100.00"));
        solicitacao.rejeitar();
        assertEquals(StatusSolicitacao.REJEITADA, solicitacao.getStatus());
        assertThrows(IllegalStateException.class, solicitacao::aprovar);
        assertThrows(IllegalStateException.class, solicitacao::rejeitar);
    }

    @Test
    void deveRejeitarDescricaoVaziaNaSolicitacao() {
        Departamento departamento = new Departamento("TI", MIL);
        assertThrows(IllegalArgumentException.class,
                () -> new Solicitacao(departamento, "", new BigDecimal("10.00")));
        assertThrows(IllegalArgumentException.class,
                () -> new Solicitacao(departamento, "   ", new BigDecimal("10.00")));
    }
}
