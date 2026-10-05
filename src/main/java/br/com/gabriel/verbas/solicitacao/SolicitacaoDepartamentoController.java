package br.com.gabriel.verbas.solicitacao;

import br.com.gabriel.verbas.solicitacao.dto.SolicitacaoRequest;
import br.com.gabriel.verbas.solicitacao.dto.SolicitacaoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class SolicitacaoDepartamentoController {

    private final SolicitacaoService service;

    public SolicitacaoDepartamentoController(SolicitacaoService service) {
        this.service = service;
    }

    @PostMapping("/departamentos/{id}/solicitacoes")
    public ResponseEntity<SolicitacaoResponse> criar(@PathVariable Long id,
            @Valid @RequestBody SolicitacaoRequest request) {
        SolicitacaoResponse response = service.criar(id, request);
        return ResponseEntity.created(URI.create("/solicitacoes/" + response.id())).body(response);
    }
}
