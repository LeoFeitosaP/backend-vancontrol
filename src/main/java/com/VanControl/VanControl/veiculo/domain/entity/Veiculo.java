package com.VanControl.VanControl.veiculo.domain.entity;

import com.VanControl.VanControl.endereco.domain.entity.Endereco;
import com.VanControl.VanControl.veiculo.domain.enums.StatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(columnDefinition = "text")
    private String enderecoInicial;

    @Embedded
    private Endereco enderecoBase;

    private Double latitude;

    private Double longitude;

    public Veiculo(
            String placa,
            String marca,
            String modelo,
            int ano,
            int capacidade,
            String renavam,
            String status
    ) {
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.capacidade = capacidade;
        this.renavam = renavam;
        this.status = StatusEnum.valueOf(status);
    }

    public void definirEndereco(Endereco endereco) {
        this.enderecoBase = endereco;
        this.enderecoInicial = endereco.formatar();
        this.latitude = null;
        this.longitude = null;
    }
}