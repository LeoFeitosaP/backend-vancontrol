package com.VanControl.VanControl.grafo.domain.entity;

import com.VanControl.VanControl.passageiro.domain.entity.Passageiro;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "passageiro_veiculo_distancia",
        uniqueConstraints = @UniqueConstraint(columnNames = {"passageiro_id", "veiculo_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassageiroVeiculoDistancia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passageiro_id", nullable = false)
    private Passageiro passageiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    private Double distanciaKm;
    private LocalDateTime calculadoEm;
}