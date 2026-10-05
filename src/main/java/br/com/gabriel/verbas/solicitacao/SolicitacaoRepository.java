package br.com.gabriel.verbas.solicitacao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long>, JpaSpecificationExecutor<Solicitacao> {

    /** Consulta escalar do id do departamento, evitando carregar a entidade antes do bloqueio. */
    @Query("select s.departamento.id from Solicitacao s where s.id = :id")
    Optional<Long> findDepartamentoIdById(@Param("id") Long id);
}
