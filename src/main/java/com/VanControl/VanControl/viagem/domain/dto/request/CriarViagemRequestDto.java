package com.VanControl.VanControl.viagem.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CriarViagemRequestDto(
        @NotBlank String codigoRota,
        @NotBlank String placaVeiculo,
        @NotBlank String cpfMotorista,
        @NotNull LocalDate dataViagem,
        @NotNull LocalTime horarioSaidaPrevisto,
        @NotNull LocalTime horarioChegadaPrevisto
) {
}
