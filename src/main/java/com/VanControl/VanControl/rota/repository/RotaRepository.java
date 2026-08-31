package com.VanControl.VanControl.rota.repository;

import com.VanControl.VanControl.rota.domain.entity.Rota;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RotaRepository extends JpaRepository<Rota, UUID> {
    Page<Rota> findByDestinoContainingIgnoreCase(String destino, Pageable pageable);

    Rota findByDescricao(String descricao);
    Optional<Rota> findByCodigoRota(String codigoRota);

    boolean existsByDescricao(String descricao);

    void deleteByCodigoRota(String codigoRota);
}
