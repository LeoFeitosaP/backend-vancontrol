package com.VanControl.VanControl.veiculo.repository;

import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {

    Veiculo findByPlaca(String placa);
}
