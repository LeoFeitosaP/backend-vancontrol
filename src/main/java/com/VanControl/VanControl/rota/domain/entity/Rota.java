package com.VanControl.VanControl.rota.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rota {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String codigoRota;

    private String descricao;
    private String destino;
    private Double distancia;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime tempoEstimado;

    public Rota(String descricao, String destino, Double distancia, LocalTime tempoEstimado) {
        this.descricao = descricao;
        this.destino = destino;
        this.distancia = distancia;
        this.tempoEstimado = tempoEstimado;
    }
}
