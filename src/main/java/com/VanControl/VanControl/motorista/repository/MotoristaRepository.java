package com.VanControl.VanControl.motorista.repository;

import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MotoristaRepository extends JpaRepository<Motorista, UUID> {
    Optional<Motorista> findByUser_Cpf(String cpf);
}
