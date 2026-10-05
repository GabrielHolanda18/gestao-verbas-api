package br.com.gabriel.verbas.historico;

import br.com.gabriel.verbas.historico.dto.HistoricoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HistoricoController {

    private final HistoricoService service;

    public HistoricoController(HistoricoService service) {
        this.service = service;
    }

    @GetMapping("/solicitacoes/{id}/historico")
    public ResponseEntity<List<HistoricoResponse>> listar(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarPorSolicitacao(id));
    }
}
