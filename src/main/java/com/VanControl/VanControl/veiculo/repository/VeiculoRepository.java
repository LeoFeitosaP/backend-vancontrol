package com.VanControl.VanControl.veiculo.repository;

import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {

    Veiculo findByPlaca(String placa);

    boolean existsByPlaca(String placa);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Veiculo v where v.idVeiculo = :id")
    Veiculo buscarParaAtualizacao(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Veiculo v where v.placa = :placa")
    Veiculo buscarPorPlacaParaAtualizacao(@Param("placa") String placa);
}