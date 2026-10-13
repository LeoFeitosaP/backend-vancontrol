package com.VanControl.VanControl.viagem.repository;

import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ViagemRepository extends JpaRepository<Viagem, UUID> {

    @EntityGraph(attributePaths = {"rota", "veiculo", "motorista.user"})
    Viagem findByCodigoViagem(String codigoViagem);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Viagem v where v.codigoViagem = :codigo")
    Viagem buscarParaAtualizacao(@Param("codigo") String codigo);

    @Override
    @EntityGraph(attributePaths = {"rota", "veiculo", "motorista.user"})
    Page<Viagem> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"rota", "veiculo", "motorista.user"})
    List<Viagem> findAll();
}