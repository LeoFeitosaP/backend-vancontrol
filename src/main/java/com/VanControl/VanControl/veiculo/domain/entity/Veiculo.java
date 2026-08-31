package com.VanControl.VanControl.veiculo.domain.entity;

import com.VanControl.VanControl.veiculo.domain.enums.StatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idVeiculo;

    @Column(unique = true)
    private String placa;
    private String marca;
    private String modelo;
    private int ano;
    private int capacidade;
    @Column(unique = true)
    private String renavam;
    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    public Veiculo(String placa, String marca, String modelo, int ano, int capacidade, String renavam, String status) {
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.capacidade = capacidade;
        this.renavam = renavam;
        this.status = StatusEnum.valueOf(status);
    }
}
