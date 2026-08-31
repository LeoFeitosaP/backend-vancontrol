package com.VanControl.VanControl.viagemPassageiro.repository;

import com.VanControl.VanControl.viagemPassageiro.domain.entity.ViagemPassageiro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ViagemPassageiroRepository extends JpaRepository<ViagemPassageiro, UUID> {

    boolean existsByViagem_IdAndPassageiro_Id(UUID viagemId, UUID passageiroId);

    long countByViagem_Id(UUID viagemId);

    List<ViagemPassageiro> findByViagem_Id(UUID viagemId);

    Page<ViagemPassageiro> findByPassageiro_Id(UUID passageiroId, Pageable pageable);

    void deleteByViagem_IdAndPassageiro_Id(UUID viagemId, UUID passageiroId);
}
