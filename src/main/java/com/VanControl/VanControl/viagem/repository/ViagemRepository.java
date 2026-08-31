package com.VanControl.VanControl.viagem.repository;

import com.VanControl.VanControl.viagem.domain.entity.Viagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ViagemRepository extends JpaRepository<Viagem, UUID> {

    Viagem findByCodigoViagem(String codigoViagem);
}
