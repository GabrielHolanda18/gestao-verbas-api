package br.com.gabriel.verbas.historico;

import br.com.gabriel.verbas.historico.dto.HistoricoResponse;
import br.com.gabriel.verbas.shared.erro.RecursoNaoEncontradoException;
import br.com.gabriel.verbas.solicitacao.Solicitacao;
import br.com.gabriel.verbas.solicitacao.SolicitacaoRepository;
import br.com.gabriel.verbas.solicitacao.StatusSolicitacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistoricoService {

    private final HistoricoSolicitacaoRepository repository;
    private final SolicitacaoRepository solicitacaoRepository;

    public HistoricoService(HistoricoSolicitacaoRepository repository, SolicitacaoRepository solicitacaoRepository) {
        this.repository = repository;
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Solicitacao solicitacao, StatusSolicitacao statusAnterior,
            String responsavel, String justificativa) {
        repository.save(new HistoricoSolicitacao(solicitacao, statusAnterior, solicitacao.getStatus(),
                responsavel, justificativa));
    }

    @Transactional(readOnly = true)
    public List<HistoricoResponse> listarPorSolicitacao(Long solicitacaoId) {
        if (!solicitacaoRepository.existsById(solicitacaoId)) {
            throw new RecursoNaoEncontradoException("Solicitação não encontrada");
        }
        return repository.findBySolicitacaoIdOrderByIdAsc(solicitacaoId).stream()
                .map(HistoricoResponse::from)
                .toList();
    }
}
