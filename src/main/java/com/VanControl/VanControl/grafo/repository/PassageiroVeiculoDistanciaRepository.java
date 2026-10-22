package com.VanControl.VanControl.grafo.repository;

import com.VanControl.VanControl.grafo.domain.entity.PassageiroVeiculoDistancia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PassageiroVeiculoDistanciaRepository extends JpaRepository<PassageiroVeiculoDistancia, UUID> {
    Optional<PassageiroVeiculoDistancia> findByPassageiro_IdAndVeiculo_idVeiculo(UUID passageiroId, UUID veiculoId);
    void deleteByPassageiro_Id(UUID passageiroId);
    void deleteByVeiculo_idVeiculo(UUID veiculoId);
}
