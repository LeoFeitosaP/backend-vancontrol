package com.VanControl.VanControl.recomendacao.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "pontos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ponto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    private Double latitude;

    private Double longitude;
}
