package br.com.gabriel.verbas.departamento;

import br.com.gabriel.verbas.departamento.dto.DepartamentoRequest;
import br.com.gabriel.verbas.departamento.dto.DepartamentoResponse;
import br.com.gabriel.verbas.shared.erro.RecursoNaoEncontradoException;
import br.com.gabriel.verbas.shared.erro.ValidacaoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DepartamentoService {

    private final DepartamentoRepository repository;

    public DepartamentoService(DepartamentoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DepartamentoResponse criar(DepartamentoRequest request) {
        validarEscala(request.orcamentoTotal(), "orcamentoTotal");
        Departamento departamento = new Departamento(request.nome(), request.orcamentoTotal());
        return DepartamentoResponse.from(repository.save(departamento));
    }

    @Transactional(readOnly = true)
    public DepartamentoResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(DepartamentoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Departamento não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<DepartamentoResponse> buscarTodos() {
        return repository.findAll().stream()
                .map(DepartamentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean existe(Long id) {
        return repository.existsById(id);
    }

    static void validarEscala(BigDecimal valor, String campo) {
        if (valor != null && valor.scale() > 2) {
            throw new ValidacaoException(campo + " deve ter no máximo 2 casas decimais");
        }
    }
}
