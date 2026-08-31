package com.VanControl.VanControl.viagem.domain.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record ViagemResponseDto(
        String codigoViagem,
        String codigoRota,
        String placaVeiculo,
        String cpfMotorista,
        LocalDate dataViagem,
        LocalTime horarioSaidaPrevisto,
        LocalTime horarioChegadaPrevisto,
        boolean viagemConcuida
) {
}
