package br.com.gabriel.verbas.solicitacao;

import br.com.gabriel.verbas.departamento.Departamento;
import br.com.gabriel.verbas.departamento.DepartamentoRepository;
import br.com.gabriel.verbas.shared.Dinheiro;
import br.com.gabriel.verbas.historico.HistoricoService;
import br.com.gabriel.verbas.shared.erro.RecursoNaoEncontradoException;
import br.com.gabriel.verbas.shared.erro.RegraNegocioException;
import br.com.gabriel.verbas.shared.erro.ValidacaoException;
import br.com.gabriel.verbas.solicitacao.dto.AprovacaoRequest;
import br.com.gabriel.verbas.solicitacao.dto.PaginaSolicitacoes;
import br.com.gabriel.verbas.solicitacao.dto.RejeicaoRequest;
import br.com.gabriel.verbas.solicitacao.dto.SolicitacaoRequest;
import br.com.gabriel.verbas.solicitacao.dto.SolicitacaoResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SolicitacaoService {

    private final SolicitacaoRepository repository;
    private final DepartamentoRepository departamentoRepository;
    private final HistoricoService historicoService;
    private final EntityManager entityManager;

    public SolicitacaoService(SolicitacaoRepository repository, DepartamentoRepository departamentoRepository,
            HistoricoService historicoService, EntityManager entityManager) {
        this.repository = repository;
        this.departamentoRepository = departamentoRepository;
        this.historicoService = historicoService;
        this.entityManager = entityManager;
    }

    @Transactional
    public SolicitacaoResponse criar(Long departamentoId, SolicitacaoRequest request) {
        Dinheiro.validarPositivo(request.valor(), "valor");
        Departamento departamento = departamentoRepository.findById(departamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Departamento não encontrado"));
        Solicitacao solicitacao = new Solicitacao(departamento, request.descricao(), request.valor());
        return SolicitacaoResponse.from(repository.save(solicitacao));
    }

    @Transactional(readOnly = true)
    public SolicitacaoResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(SolicitacaoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));
    }

    @Transactional(readOnly = true)
    public PaginaSolicitacoes listar(Long departamentoId, StatusSolicitacao status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidacaoException("page deve ser >= 0 e size deve estar entre 1 e 100");
        }
        if (departamentoId != null) {
            if (departamentoId <= 0) {
                throw new ValidacaoException("departamentoId deve ser positivo");
            }
            if (!departamentoRepository.existsById(departamentoId)) {
                throw new RecursoNaoEncontradoException("Departamento não encontrado");
            }
        }
        Specification<Solicitacao> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (departamentoId != null) {
                predicates.add(cb.equal(root.get("departamento").get("id"), departamentoId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Solicitacao> resultado = repository.findAll(spec, pageable);
        List<SolicitacaoResponse> conteudo = resultado.getContent().stream()
                .map(SolicitacaoResponse::from)
                .toList();
        return new PaginaSolicitacoes(conteudo, resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    @Transactional
    public SolicitacaoResponse aprovar(Long id, AprovacaoRequest request) {
        return decidir(id, true, request.responsavel(), null);
    }

    @Transactional
    public SolicitacaoResponse rejeitar(Long id, RejeicaoRequest request) {
        return decidir(id, false, request.responsavel(), request.justificativa());
    }

    private SolicitacaoResponse decidir(Long id, boolean aprovar, String responsavel, String justificativa) {
        Long departamentoId = repository.findDepartamentoIdById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));
        Departamento departamento = departamentoRepository.findByIdComBloqueio(departamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Departamento não encontrado"));
        Solicitacao solicitacao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));
        if (!solicitacao.isPendente()) {
            throw new RegraNegocioException("Solicitação já foi decidida");
        }
        StatusSolicitacao statusAnterior = solicitacao.getStatus();
        if (aprovar) {
            if (!departamento.temSaldoPara(solicitacao.getValor())) {
                throw new RegraNegocioException("Saldo insuficiente no departamento para aprovar a solicitação");
            }
            departamento.reservar(solicitacao.getValor());
            solicitacao.aprovar();
        } else {
            solicitacao.rejeitar();
        }
        entityManager.flush();
        historicoService.registrar(solicitacao, statusAnterior, responsavel, justificativa);
        return SolicitacaoResponse.from(solicitacao);
    }
}
