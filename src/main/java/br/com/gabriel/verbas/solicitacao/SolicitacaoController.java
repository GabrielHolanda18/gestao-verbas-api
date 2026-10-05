package br.com.gabriel.verbas.solicitacao;

import br.com.gabriel.verbas.solicitacao.dto.AprovacaoRequest;
import br.com.gabriel.verbas.solicitacao.dto.PaginaSolicitacoes;
import br.com.gabriel.verbas.solicitacao.dto.RejeicaoRequest;
import br.com.gabriel.verbas.solicitacao.dto.SolicitacaoRequest;
import br.com.gabriel.verbas.solicitacao.dto.SolicitacaoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/solicitacoes")
@Validated
public class SolicitacaoController {

    private final SolicitacaoService service;

    public SolicitacaoController(SolicitacaoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PaginaSolicitacoes> listar(
            @RequestParam(required = false) @Positive(message = "departamentoId deve ser positivo") Long departamentoId,
            @RequestParam(required = false) StatusSolicitacao status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page deve ser maior ou igual a 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size deve ser maior ou igual a 1") int size) {
        return ResponseEntity.ok(service.listar(departamentoId, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping("/{id}/aprovar")
    public ResponseEntity<SolicitacaoResponse> aprovar(@PathVariable Long id,
            @Valid @RequestBody AprovacaoRequest request) {
        return ResponseEntity.ok(service.aprovar(id, request));
    }

    @PostMapping("/{id}/rejeitar")
    public ResponseEntity<SolicitacaoResponse> rejeitar(@PathVariable Long id,
            @Valid @RequestBody RejeicaoRequest request) {
        return ResponseEntity.ok(service.rejeitar(id, request));
    }
}
