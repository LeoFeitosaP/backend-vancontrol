package com.VanControl.VanControl.passageiro.repository;

import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PassageiroRepository extends JpaRepository<Passageiro, UUID> {
    Optional<Passageiro> findByUser_Id(UUID user);
    Optional<Passageiro> findByUser_Cpf(String cpf);
}
