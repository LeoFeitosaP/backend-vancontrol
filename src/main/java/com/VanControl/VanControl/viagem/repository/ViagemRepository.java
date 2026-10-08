package com.VanControl.VanControl.viagem.repository;

import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ViagemRepository extends JpaRepository<Viagem, UUID> {

    @EntityGraph(attributePaths = {"rota", "veiculo", "motorista.user"})
    Viagem findByCodigoViagem(String codigoViagem);

    @Override
    @EntityGraph(attributePaths = {"rota", "veiculo", "motorista.user"})
    Page<Viagem> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"rota", "veiculo", "motorista.user"})
    List<Viagem> findAll();
}