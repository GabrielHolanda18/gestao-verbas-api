package br.com.gabriel.verbas.historico;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoSolicitacaoRepository extends JpaRepository<HistoricoSolicitacao, Long> {

    List<HistoricoSolicitacao> findBySolicitacaoIdOrderByIdAsc(Long solicitacaoId);
}
