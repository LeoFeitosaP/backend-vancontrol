package com.VanControl.VanControl.viagem.domain.entity;

import com.VanControl.VanControl.viagemPassageiro.domain.entity.ViagemPassageiro;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Viagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String codigoRota;
    private String placaVeiculo;
    private String documentoMotorista;

    private String codigoViagem;

    private LocalDate dataViagem;
    private LocalTime horarioSaidaPrevisto;
    private LocalTime horarioChegadaPrevisto;
    private boolean viagemConcluida;

    @OneToMany(mappedBy = "viagem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ViagemPassageiro> passageiros = new ArrayList<>();

    public Viagem(String codigoRota, String placaVeiculo, String documentoMotorista, LocalDate dataViagem,
                  LocalTime horarioSaidaPrevisto, LocalTime horarioChegadaPrevisto) {
        this.codigoRota = codigoRota;
        this.placaVeiculo = placaVeiculo;
        this.documentoMotorista = documentoMotorista;
        this.dataViagem = dataViagem;
        this.horarioSaidaPrevisto = horarioSaidaPrevisto;
        this.horarioChegadaPrevisto = horarioChegadaPrevisto;
    }
}
