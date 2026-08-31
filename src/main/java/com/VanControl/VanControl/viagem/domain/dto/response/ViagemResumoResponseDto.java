package com.VanControl.VanControl.viagem.domain.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record ViagemResumoResponseDto(
        String codigoViagem,
        String codigoRota,
        String placaVeiculo,
        LocalDate dataViagem,
        LocalTime horarioSaidaPrevisto,
        LocalTime horarioChegadaPrevisto,
        boolean viagemConcluida
) {
}
