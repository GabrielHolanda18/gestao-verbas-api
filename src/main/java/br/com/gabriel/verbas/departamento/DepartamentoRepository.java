package br.com.gabriel.verbas.departamento;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    /**
     * Bloqueio pessimista de escrita no departamento. Deve ser a PRIMEIRA operacao
     * de qualquer decisao (aprovacao ou rejeicao) para serializar o acesso ao saldo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Departamento d where d.id = :id")
    Optional<Departamento> findByIdComBloqueio(@Param("id") Long id);
}
