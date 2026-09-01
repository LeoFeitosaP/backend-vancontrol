package com.VanControl.VanControl.recomendacao.repository;

import com.VanControl.VanControl.recomendacao.domain.entity.Ponto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PontoRepository extends JpaRepository<Ponto, UUID> {

    Optional<Ponto> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}