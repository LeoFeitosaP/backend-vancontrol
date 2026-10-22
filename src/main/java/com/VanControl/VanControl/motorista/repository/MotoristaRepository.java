package com.VanControl.VanControl.motorista.repository;

import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MotoristaRepository extends JpaRepository<Motorista, UUID> {

    Optional<Motorista> findByUser_Cpf(String cpf);

    @Query("""
            select m from Motorista m join fetch m.user u
            where replace(replace(trim(u.cpf), '.', ''), '-', '') = :cpf
            """)
    List<Motorista> findAllByCpfNormalizado(@Param("cpf") String cpf);
}