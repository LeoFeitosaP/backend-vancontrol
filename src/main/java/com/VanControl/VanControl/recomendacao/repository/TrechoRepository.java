package com.VanControl.VanControl.recomendacao.repository;

import com.VanControl.VanControl.recomendacao.domain.entity.Trecho;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrechoRepository extends JpaRepository<Trecho, UUID> {

    @EntityGraph(attributePaths = {"origem", "destino"})
    List<Trecho> findAllByAtivoTrue();
}