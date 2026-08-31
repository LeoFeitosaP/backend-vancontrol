package com.VanControl.VanControl.viagem.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalTime;

public record CriarViagemRequestDto(
        @NotBlank String codigoRota,
        @NotBlank String placaVeiculo,
        @NotBlank String cpfMotorista,
        @NotBlank LocalDate dataViagem,
        @NotBlank LocalTime horarioSaidaPrevisto,
        @NotBlank LocalTime horarioChegadaPrevisto
) {
}
