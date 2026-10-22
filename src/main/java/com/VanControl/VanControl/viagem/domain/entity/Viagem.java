package com.VanControl.VanControl.viagem.domain.entity;

import com.VanControl.VanControl.motorista.domain.entity.Motorista;
import com.VanControl.VanControl.rota.domain.entity.Rota;
import com.VanControl.VanControl.veiculo.domain.entity.Veiculo;
import com.VanControl.VanControl.viagemPassageiro.domain.entity.ViagemPassageiro;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rota_id", nullable = false, updatable = false)
    private Rota rota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false, updatable = false)
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false, updatable = false)
    private Motorista motorista;

    // Cópias históricas: não localizam as entidades relacionadas.
    @Column(nullable = false, updatable = false)
    private String codigoRota;

    @Column(nullable = false, updatable = false)
    private String placaVeiculo;

    @Column(nullable = false, updatable = false)
    private String documentoMotorista;

    @Column(nullable = false, unique = true, updatable = false)
    private String codigoViagem;

    private LocalDate dataViagem;
    private LocalTime horarioSaidaPrevisto;
    private LocalTime horarioChegadaPrevisto;
    private boolean viagemConcluida;

    @OneToMany(
            mappedBy = "viagem",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ViagemPassageiro> passageiros = new ArrayList<>();

    public Viagem(
            Rota rota,
            Veiculo veiculo,
            Motorista motorista,
            LocalDate dataViagem,
            LocalTime horarioSaidaPrevisto,
            LocalTime horarioChegadaPrevisto
    ) {
        this.rota = Objects.requireNonNull(rota);
        this.veiculo = Objects.requireNonNull(veiculo);
        this.motorista = Objects.requireNonNull(motorista);

        this.codigoRota = Objects.requireNonNull(rota.getCodigoRota());
        this.placaVeiculo = Objects.requireNonNull(veiculo.getPlaca());

        this.documentoMotorista = Objects.requireNonNull(
                motorista.getUser().getCpf()
        ).trim().replace(".", "").replace("-", "");

        this.dataViagem = dataViagem;
        this.horarioSaidaPrevisto = horarioSaidaPrevisto;
        this.horarioChegadaPrevisto = horarioChegadaPrevisto;
    }
}