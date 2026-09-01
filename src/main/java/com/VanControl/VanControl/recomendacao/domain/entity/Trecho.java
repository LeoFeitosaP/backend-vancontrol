package com.VanControl.VanControl.recomendacao.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "trechos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_trecho_origem_destino",
                        columnNames = {"origem_id", "destino_id"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trecho {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origem_id", nullable = false)
    private Ponto origem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destino_id", nullable = false)
    private Ponto destino;

    @Column(nullable = false)
    private double distanciaKm;

    @Column(nullable = false)
    private long tempoEstimadoMinutos;

    @Column(nullable = false)
    @Builder.Default
    private boolean bidirecional = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;
}